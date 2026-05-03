package net.dkly.aplife.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HolidaySetDto(
    val active: Boolean = true,
    val year: Int,
    val remarks: String? = null,
    val holidays: List<HolidayDto> = emptyList(),
)

@Serializable
data class HolidayDto(
    val id: Int? = null,
    @SerialName("holiday_name") val name: String,
    @SerialName("holiday_description") val description: String? = null,
    @SerialName("holiday_start_date") val startDate: String,
    @SerialName("holiday_end_date") val endDate: String,
    @SerialName("holiday_people_affected") val peopleAffected: String? = null,
)

data class Holiday(
    val name: String,
    val description: String?,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val peopleAffected: String,
    val year: Int,
)
