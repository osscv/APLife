package net.dkly.aplife.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lecturer(
    @SerialName("CODE") val code: String? = null,
    @SerialName("DEPARTMENT") val department: String? = null,
    @SerialName("DEPARTMENT2") val department2: String? = null,
    @SerialName("DEPARTMENT3") val department3: String? = null,
    @SerialName("EMAIL") val email: String? = null,
    @SerialName("STAFFEMAIL") val staffEmail: String? = null,
    @SerialName("EXTENSION") val extension: String? = null,
    @SerialName("FULLNAME") val fullName: String,
    @SerialName("ID") val id: String? = null,
    @SerialName("LOCATION") val location: String? = null,
    @SerialName("PHOTO") val photo: String? = null,
    @SerialName("RefNo") val refNo: Int? = null,
    @SerialName("TITLE") val title: String? = null,
)
