package net.dkly.aplife.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuixCompany(
    @SerialName("company_id") val companyId: String? = null,
    @SerialName("company_name") val companyName: String? = null,
    @SerialName("customer_type") val customerType: String? = null,
    @SerialName("company_departments") val departments: List<Department> = emptyList(),
)

@Serializable
data class Department(
    @SerialName("dept_id") val id: String,
    @SerialName("dept_name") val name: String,
    @SerialName("dept_email") val email: String? = null,
    @SerialName("dept_phone") val phones: List<String> = emptyList(),
    val shifts: Map<String, List<Shift>> = emptyMap(),
)

@Serializable
data class Shift(
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
)
