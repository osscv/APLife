package net.dkly.aplife.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimetableEntry(
    @SerialName("INTAKE") val intake: String,
    @SerialName("MODID") val moduleId: String? = null,
    @SerialName("MODULE_NAME") val moduleName: String,
    @SerialName("DAY") val day: String? = null,
    @SerialName("LOCATION") val location: String? = null,
    @SerialName("ROOM") val room: String? = null,
    @SerialName("LECTID") val lecturerId: String? = null,
    @SerialName("NAME") val lecturerName: String? = null,
    @SerialName("SAMACCOUNTNAME") val lecturerAccount: String? = null,
    @SerialName("DATESTAMP") val dateStamp: String? = null,
    @SerialName("DATESTAMP_ISO") val dateStampIso: String? = null,
    @SerialName("TIME_FROM") val timeFrom: String? = null,
    @SerialName("TIME_TO") val timeTo: String? = null,
    @SerialName("TIME_FROM_ISO") val timeFromIso: String,
    @SerialName("TIME_TO_ISO") val timeToIso: String,
    @SerialName("GROUPING") val grouping: String? = null,
    @SerialName("CLASS_CODE") val classCode: String? = null,
    @SerialName("COLOR") val color: String? = null,
)
