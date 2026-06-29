package com.example.kubmi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kubmi.data.local.entity.AboutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AboutDao {
    @Query("SELECT * FROM about WHERE id = 0 LIMIT 1")
    fun getAboutContent(): Flow<AboutEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAboutContent(about: AboutEntity)
    
    @Query("DELETE FROM about")
    suspend fun clearAboutContent()
}