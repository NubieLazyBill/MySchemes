package com.example.myschemes.data.database

import androidx.room.*
import com.example.myschemes.data.model.Scheme

@Dao
interface SchemeDao {
    @Query("SELECT * FROM schemes ORDER BY nextRevisionDate ASC")
    suspend fun getAllSchemes(): List<Scheme>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schemes: List<Scheme>)

    @Query("DELETE FROM schemes")
    suspend fun deleteAll()

    @Query("SELECT * FROM schemes WHERE nextRevisionDate <= :date")
    suspend fun getSchemesExpiringBefore(date: Long): List<Scheme>
}