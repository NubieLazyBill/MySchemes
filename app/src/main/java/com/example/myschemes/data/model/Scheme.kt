package com.example.myschemes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schemes")
data class Scheme(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val itemNumber: Int? = null,
    val cellNumber: String? = null,
    val equipmentName: String,
    val lastRevisionDate: Long,
    val nextRevisionDate: Long,
    val schemeNumber: String? = null
)