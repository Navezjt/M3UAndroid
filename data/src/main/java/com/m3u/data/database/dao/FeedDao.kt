package com.m3u.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.m3u.data.database.entity.Feed
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feed: Feed): Long

    @Delete
    suspend fun delete(vararg feed: Feed)

    @Query("SELECT * FROM feeds WHERE url = :url")
    suspend fun getByUrl(url: String): Feed?

    @Query("SELECT * FROM feeds ORDER BY title")
    fun observeAll(): Flow<List<Feed>>

    @Query("SELECT * FROM feeds WHERE url = :url ORDER BY title")
    fun observeByUrl(url: String): Flow<Feed?>

    @Query("UPDATE feeds SET title = :target WHERE url = :url")
    suspend fun rename(url: String, target: String)
}