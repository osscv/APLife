package net.dkly.aplife.data

import kotlinx.serialization.Serializable

@Serializable
data class PersonalEvent(
    val id: Long = 0,
    val type: PersonalEventType,
    val title: String,
    val location: String? = null,
    val notes: String = "",
    val startMs: Long,
    val endMs: Long,
    val lecturerCode: String? = null,
    val lecturerSam: String? = null,
    val lecturerName: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
)

enum class PersonalEventType(val label: String) {
    REPLACEMENT_CLASS("Replacement Class"),
    APPOINTMENT("Appointment with Lecturer"),
    PRESENTATION("Presentation"),
    EVENT("Event"),
    STUDY_GROUP("Study Group"),
    OTHER("Other"),
}
