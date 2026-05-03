package net.dkly.aplife.data

import kotlinx.serialization.Serializable

@Serializable
data class LecturerScheduleEntry(
    val time: String,
    val duration: Double,
    val intakes: List<String> = emptyList(),
    val location: String? = null,
    val module: String? = null,
    val room: String? = null,
)
