package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DownloadedItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
  @Query("SELECT * FROM downloaded_items ORDER BY downloadTimestamp DESC")
  fun getAllDownloads(): Flow<List<DownloadedItem>>

  @Query("SELECT * FROM downloaded_items WHERE id = :id LIMIT 1")
  suspend fun getDownloadById(id: Long): DownloadedItem?

  @Query("SELECT * FROM downloaded_items WHERE isVideo = :isVideo ORDER BY downloadTimestamp DESC")
  fun getDownloadsByType(isVideo: Boolean): Flow<List<DownloadedItem>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDownload(item: DownloadedItem): Long

  @Delete
  suspend fun deleteDownload(item: DownloadedItem)

  @Query("DELETE FROM downloaded_items WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("SELECT COUNT(*) FROM downloaded_items")
  suspend fun getCount(): Int
}
