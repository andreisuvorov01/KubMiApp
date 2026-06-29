package com.example.kubmi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kubmi.data.local.entity.ScheduleCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleCacheDao {
    @Query("SELECT * FROM schedule_cache WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ScheduleCacheEntity?>

    @Query("SELECT * FROM schedule_cache WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ScheduleCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScheduleCacheEntity)

    @Query("DELETE FROM schedule_cache WHERE timestamp < :minTimestamp")
    suspend fun deleteOlderThan(minTimestamp: Long)
}
