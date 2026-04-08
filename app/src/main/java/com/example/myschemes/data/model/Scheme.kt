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

    // Новые поля для чек-листа шкафа
    val cabinetName: String? = null,           // дисп.наим. Шкафа
    val lockIntegrity: Boolean = false,        // Целостность замков
    val sealIntegrity: Boolean = false,        // Уплотнение шкафа
    val cableEntries: Boolean = false,         // Заходы кабелей в защите
    val switchesName: String? = null,          // дисп.наим.автоматов, рубильников
    val noBareWires: Boolean = false,          // Не подкл. оголенные жилы
    val addressLabels: Boolean = false,        // Наличие адресных бирок
    val terminalsIntegrity: Boolean = false,   // Целостность клеммников
    val painting: Boolean = false,             // окраска
    val inventoryNumber: String? = null,       // Инвентарный номер
    val heating: Boolean = false,              // Обогрев
    val grounding: Boolean = false,            // Заземление

    // Фото
    val photoPath: String? = null              // путь к сохранённому фото
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