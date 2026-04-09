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
    val schemeNumber: String? = null,

    // Чек-лист
    val cabinetNameChecked: Boolean = false,
    val switchesNameChecked: Boolean = false,
    val inventoryNumberChecked: Boolean = false,
    val lockIntegrity: Boolean = false,
    val sealIntegrity: Boolean = false,
    val cableEntries: Boolean = false,
    val noBareWires: Boolean = false,
    val addressLabels: Boolean = false,
    val terminalsIntegrity: Boolean = false,
    val painting: Boolean = false,
    val heating: Boolean = false,
    val grounding: Boolean = false,

    // Фото для каждого пункта
    val cabinetNamePhoto: String? = null,
    val switchesNamePhoto: String? = null,
    val inventoryNumberPhoto: String? = null,
    val lockIntegrityPhoto: String? = null,
    val sealIntegrityPhoto: String? = null,
    val cableEntriesPhoto: String? = null,
    val noBareWiresPhoto: String? = null,
    val addressLabelsPhoto: String? = null,
    val terminalsIntegrityPhoto: String? = null,
    val paintingPhoto: String? = null,
    val heatingPhoto: String? = null,
    val groundingPhoto: String? = null,

    // Общее фото шкафа
    val generalPhoto: String? = null
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