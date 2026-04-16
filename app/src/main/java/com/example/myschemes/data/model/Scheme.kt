package com.example.myschemes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.myschemes.data.database.Converters
import java.io.Serializable

@Entity(tableName = "schemes")
@TypeConverters(Converters::class)
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

    // Фото для каждого пункта (список путей)
    val cabinetNamePhotos: List<String> = emptyList(),
    val switchesNamePhotos: List<String> = emptyList(),
    val inventoryNumberPhotos: List<String> = emptyList(),
    val lockIntegrityPhotos: List<String> = emptyList(),
    val sealIntegrityPhotos: List<String> = emptyList(),
    val cableEntriesPhotos: List<String> = emptyList(),
    val noBareWiresPhotos: List<String> = emptyList(),
    val addressLabelsPhotos: List<String> = emptyList(),
    val terminalsIntegrityPhotos: List<String> = emptyList(),
    val paintingPhotos: List<String> = emptyList(),
    val heatingPhotos: List<String> = emptyList(),
    val groundingPhotos: List<String> = emptyList(),

    // Общее фото шкафа (тоже список)
    val generalPhotos: List<String> = emptyList(),

    // Примечания для каждого пункта
    val cabinetNameNote: String? = null,
    val switchesNameNote: String? = null,
    val inventoryNumberNote: String? = null,
    val lockIntegrityNote: String? = null,
    val sealIntegrityNote: String? = null,
    val cableEntriesNote: String? = null,
    val noBareWiresNote: String? = null,
    val addressLabelsNote: String? = null,
    val terminalsIntegrityNote: String? = null,
    val paintingNote: String? = null,
    val heatingNote: String? = null,
    val groundingNote: String? = null,
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

enum class ChecklistStatus {
    NORMAL,     // зелёная галочка
    WARNING,    // жёлтый вопросик
    FAIL        // красный крестик
}