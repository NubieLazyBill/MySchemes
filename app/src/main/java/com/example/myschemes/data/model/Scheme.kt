package com.example.myschemes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

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
) : Serializable {

    fun getStatus(): SchemeStatus {
        val today = System.currentTimeMillis()
        val daysLeft = ((nextRevisionDate - today) / (1000 * 60 * 60 * 24)).toInt()
        return when {
            daysLeft < 0 -> SchemeStatus.EXPIRED
            daysLeft <= 30 -> SchemeStatus.EXPIRING
            else -> SchemeStatus.ACTIVE
        }
    }
}

enum class SchemeStatus {
    ACTIVE, EXPIRING, EXPIRED
}