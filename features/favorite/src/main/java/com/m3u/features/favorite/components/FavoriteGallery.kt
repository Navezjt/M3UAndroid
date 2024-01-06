package com.m3u.features.favorite.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import com.m3u.core.architecture.pref.LocalPref
import com.m3u.data.database.entity.Stream
import com.m3u.material.ktx.plus
import com.m3u.material.model.LocalSpacing
import com.m3u.ui.LocalHelper
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun FavouriteGallery(
    contentPadding: PaddingValues,
    streams: ImmutableList<Stream>,
    zapping: Stream?,
    rowCount: Int,
    navigateToStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pref = LocalPref.current
    if (pref.compact) {
        CompactFavouriteGalleryImpl(
            contentPadding = contentPadding,
            streams = streams,
            zapping = zapping,
            rowCount = rowCount,
            navigateToStream = navigateToStream,
            modifier = modifier
        )
    } else {
        FavouriteGalleryImpl(
            contentPadding = contentPadding,
            streams = streams,
            zapping = zapping,
            rowCount = rowCount,
            navigateToStream = navigateToStream,
            modifier = modifier
        )
    }
}

@Composable
private fun FavouriteGalleryImpl(
    contentPadding: PaddingValues,
    streams: ImmutableList<Stream>,
    zapping: Stream?,
    rowCount: Int,
    navigateToStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val pref = LocalPref.current
    val helper = LocalHelper.current
    val configuration = LocalConfiguration.current

    val type = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    if (type != Configuration.UI_MODE_TYPE_TELEVISION) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(rowCount),
            verticalItemSpacing = spacing.medium,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(spacing.medium) + contentPadding,
            modifier = modifier.fillMaxSize(),
        ) {
            items(
                items = streams,
                key = { it.id },
                contentType = { it.cover.isNullOrEmpty() }
            ) { stream ->
                FavoriteItem(
                    stream = stream,
                    noPictureMode = pref.noPictureMode,
                    zapping = zapping == stream,
                    onClick = {
                        helper.play(stream.url)
                        navigateToStream()
                    },
                    onLongClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        TvLazyVerticalGrid(
            columns = TvGridCells.Fixed(rowCount),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(spacing.medium) + contentPadding,
            modifier = modifier.fillMaxSize(),
        ) {
            items(
                items = streams,
                key = { it.id },
                contentType = { it.cover.isNullOrEmpty() }
            ) { stream ->
                FavoriteItem(
                    stream = stream,
                    noPictureMode = pref.noPictureMode,
                    zapping = zapping == stream,
                    onClick = {
                        helper.play(stream.url)
                        navigateToStream()
                    },
                    onLongClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CompactFavouriteGalleryImpl(
    contentPadding: PaddingValues,
    streams: ImmutableList<Stream>,
    zapping: Stream?,
    rowCount: Int,
    navigateToStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val pref = LocalPref.current
    val helper = LocalHelper.current
    val configuration = LocalConfiguration.current

    val type = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    if (type != Configuration.UI_MODE_TYPE_TELEVISION) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(rowCount),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = contentPadding,
            modifier = modifier.fillMaxSize(),
        ) {
            items(
                items = streams,
                key = { it.id },
                contentType = { it.cover.isNullOrEmpty() }
            ) { stream ->
                CompatFavoriteItem(
                    stream = stream,
                    noPictureMode = pref.noPictureMode,
                    zapping = zapping == stream,
                    onClick = {
                        helper.play(stream.url)
                        navigateToStream()
                    },
                    onLongClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        TvLazyVerticalGrid(
            columns = TvGridCells.Fixed(rowCount),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = contentPadding,
            modifier = modifier.fillMaxSize(),
        ) {
            items(
                items = streams,
                key = { it.id },
                contentType = { it.cover.isNullOrEmpty() }
            ) { stream ->
                CompatFavoriteItem(
                    stream = stream,
                    noPictureMode = pref.noPictureMode,
                    zapping = zapping == stream,
                    onClick = {
                        helper.play(stream.url)
                        navigateToStream()
                    },
                    onLongClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}