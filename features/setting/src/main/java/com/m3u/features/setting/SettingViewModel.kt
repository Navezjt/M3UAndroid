package com.m3u.features.setting

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.workDataOf
import com.m3u.core.architecture.Publisher
import com.m3u.core.architecture.dispatcher.Dispatcher
import com.m3u.core.architecture.dispatcher.M3uDispatchers.IO
import com.m3u.core.architecture.logger.Logger
import com.m3u.core.architecture.logger.Profiles
import com.m3u.core.architecture.logger.install
import com.m3u.core.architecture.preferences.Preferences
import com.m3u.core.unit.DataUnit
import com.m3u.core.unit.KB
import com.m3u.core.util.basic.startWithHttpScheme
import com.m3u.data.api.LocalPreparedService
import com.m3u.data.database.dao.ColorPackDao
import com.m3u.data.database.model.ColorPack
import com.m3u.data.database.model.DataSource
import com.m3u.data.database.model.Playlist
import com.m3u.data.database.model.Stream
import com.m3u.data.parser.xtream.XtreamInput
import com.m3u.data.repository.playlist.PlaylistRepository
import com.m3u.data.repository.stream.StreamRepository
import com.m3u.data.service.Messager
import com.m3u.data.service.PlayerManager
import com.m3u.data.worker.BackupWorker
import com.m3u.data.worker.RestoreWorker
import com.m3u.data.worker.SubscriptionWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val streamRepository: StreamRepository,
    private val workManager: WorkManager,
    preferences: Preferences,
    private val messager: Messager,
    private val localService: LocalPreparedService,
    private val playerManager: PlayerManager,
    publisher: Publisher,
    colorPackDao: ColorPackDao,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    delegate: Logger
) : ViewModel() {
    private val logger = delegate.install(Profiles.VIEWMODEL_SETTING)

    internal val hiddenStreams: StateFlow<List<Stream>> = streamRepository
        .observeAllHidden()
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(5_000L)
        )

    internal val hiddenCategoriesWithPlaylists: StateFlow<List<Pair<Playlist, String>>> =
        playlistRepository
            .observeAll()
            .map { playlists ->
                playlists
                    .filter { it.hiddenCategories.isNotEmpty() }
                    .flatMap { playlist -> playlist.hiddenCategories.map { playlist to it } }
            }
            .flowOn(ioDispatcher)
            .stateIn(
                scope = viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(5_000L)
            )

    internal fun onUnhidePlaylistCategory(playlistUrl: String, group: String) {
        viewModelScope.launch {
            playlistRepository.hideOrUnhideCategory(playlistUrl, group)
        }
    }

    internal val colorPacks: StateFlow<List<ColorPack>> = combine(
        colorPackDao.observeAllColorPacks().catch { emit(emptyList()) },
        snapshotFlow { preferences.followSystemTheme }
    ) { all, followSystemTheme -> if (followSystemTheme) all.filter { !it.isDark } else all }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    internal fun onClipboard(url: String) {
        val title = run {
            val filePath = url.split("/")
            val fileSplit = filePath.lastOrNull()?.split(".") ?: emptyList()
            fileSplit.firstOrNull() ?: "Playlist_${System.currentTimeMillis()}"
        }
        this.title = Uri.decode(title)
        this.url = Uri.decode(url)
        when (selected) {
            is DataSource.Xtream -> {
                val input = XtreamInput.decodeFromPlaylistUrlOrNull(url) ?: return
                basicUrl = input.basicUrl
                username = input.username
                password = input.password
                this.title = Uri.decode("Xtream_${Clock.System.now().toEpochMilliseconds()}")
            }

            else -> {}
        }
    }

    internal fun onUnhideStream(streamId: Int) {
        val hidden = hiddenStreams.value.find { it.id == streamId }
        if (hidden != null) {
            viewModelScope.launch {
                streamRepository.hide(streamId, false)
            }
        }
    }

    internal fun subscribe() {
        if (title.isEmpty()) {
            messager.emit(SettingMessage.EmptyTitle)
            return
        }
        val urlOrUri = uri
            .takeIf { uri != Uri.EMPTY }?.toString().orEmpty()
            .takeIf { localStorage }
            ?: url

        val basicUrl = if (basicUrl.startWithHttpScheme()) basicUrl
        else "http://$basicUrl"

        when {
            forTv -> {
                viewModelScope.launch {
                    localService.subscribe(
                        title,
                        urlOrUri,
                        basicUrl,
                        username,
                        password,
                        epg.ifBlank { null },
                        selected
                    )
                }
            }

            else -> when (selected) {
                DataSource.M3U -> SubscriptionWorker.m3u(workManager, title, urlOrUri, epg)
                DataSource.Xtream -> SubscriptionWorker.xtream(
                    workManager,
                    title,
                    urlOrUri,
                    basicUrl,
                    username,
                    password
                )
                else -> return
            }
        }
        messager.emit(SettingMessage.Enqueued)
        resetAllInputs()
    }

    internal val backingUpOrRestoring: StateFlow<BackingUpAndRestoringState> = workManager
        .getWorkInfosFlow(
            WorkQuery.fromStates(
                WorkInfo.State.RUNNING,
                WorkInfo.State.ENQUEUED
            )
        )
        .mapLatest { infos ->
            var backingUp = false
            var restoring = false
            for (info in infos) {
                if (backingUp && restoring) break
                for (tag in info.tags) {
                    if (backingUp && restoring) break
                    if (tag == BackupWorker.TAG) backingUp = true
                    if (tag == RestoreWorker.TAG) restoring = true
                }
            }
            BackingUpAndRestoringState.of(backingUp, restoring)
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            // determine ui button enabled or not
            // both as default
            initialValue = BackingUpAndRestoringState.BOTH,
            started = SharingStarted.WhileSubscribed(5000)
        )

    fun backup(uri: Uri) {
        workManager.cancelAllWorkByTag(BackupWorker.TAG)
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(
                workDataOf(
                    BackupWorker.INPUT_URI to uri.toString()
                )
            )
            .addTag(BackupWorker.TAG)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueue(request)
        messager.emit(SettingMessage.BackingUp)
    }

    fun restore(uri: Uri) {
        workManager.cancelAllWorkByTag(RestoreWorker.TAG)
        val request = OneTimeWorkRequestBuilder<RestoreWorker>()
            .setInputData(
                workDataOf(
                    RestoreWorker.INPUT_URI to uri.toString()
                )
            )
            .addTag(RestoreWorker.TAG)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueue(request)
        messager.emit(SettingMessage.Restoring)
    }

    internal val cacheSpace: StateFlow<DataUnit> = playerManager
        .cacheSpace
        .map { DataUnit.of(it) }
        .stateIn(
            scope = viewModelScope,
            initialValue = 0.0.KB,
            started = SharingStarted.Lazily
        )

    private fun resetAllInputs() {
        title = ""
        url = ""
        uri = Uri.EMPTY
        basicUrl = ""
        username = ""
        password = ""
        epg = ""
    }

    internal fun clearCache() {
        playerManager.clearCache()
    }

    internal var forTv by mutableStateOf(false)
    internal var selected: DataSource by mutableStateOf(DataSource.M3U)
    internal var basicUrl by mutableStateOf("")
    internal var username by mutableStateOf("")
    internal var password by mutableStateOf("")
    internal var epg by mutableStateOf("")

    val versionName: String = publisher.versionName
    val versionCode: Int = publisher.versionCode
    var title: String by mutableStateOf("")
    var url: String by mutableStateOf("")
    var uri: Uri by mutableStateOf(Uri.EMPTY)
    var localStorage: Boolean by mutableStateOf(false)
}