package com.bykea.locationtracker.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocationEntity): Long

    @Query("SELECT * FROM location_history ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location_history ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun latest(): LocationEntity?

    @Query("SELECT COUNT(*) FROM location_history")
    suspend fun count(): Int

    @Query("DELETE FROM location_history")
    suspend fun clear()
}
