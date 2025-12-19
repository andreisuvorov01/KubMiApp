package com.example.kubmi.data.local.dao

import androidx.room.*
import com.example.kubmi.data.local.entity.NewsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news WHERE isPdfSlide = 0 ORDER BY timestamp DESC")
    fun getAllNews(): Flow<List<NewsEntity>>

    @Query("SELECT * FROM news WHERE isPdfSlide = 0 ORDER BY timestamp DESC")
    suspend fun getAllNewsSync(): List<NewsEntity>

    @Query("SELECT * FROM news WHERE isPdfSlide = 1 ORDER BY timestamp DESC")
    fun getPdfSlides(): Flow<List<NewsEntity>>

    @Query("SELECT * FROM news WHERE id = :id")
    fun observeNewsById(id: String): Flow<NewsEntity?>

    @Query("SELECT * FROM news WHERE id = :id")
    suspend fun getNewsById(id: String): NewsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(news: NewsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(news: List<NewsEntity>)

    @Query("DELETE FROM news WHERE id NOT IN (:ids)")
    suspend fun deleteNotInIds(ids: List<String>)

    @Query("DELETE FROM news")
    suspend fun deleteAll()
}
