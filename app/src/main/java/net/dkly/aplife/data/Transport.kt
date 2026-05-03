package net.dkly.aplife.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransportLocation(
    val id: Int,
    val name: String,
    val address: String? = null,
    val color: String? = null,
    @SerialName("contact_number") val contactNumber: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("pickup_latitude") val pickupLatitude: String? = null,
    @SerialName("pickup_longitude") val pickupLongitude: String? = null,
    val type: String? = null,
)

@Serializable
data class TransportSchedule(
    val id: Int,
    val name: String? = null,
    val active: Boolean = true,
    @SerialName("applicable_from") val applicableFrom: String? = null,
    @SerialName("applicable_to") val applicableTo: String? = null,
    val default: Boolean = false,
    val trips: List<TransportTrip> = emptyList(),
)

@Serializable
data class TransportTrip(
    val id: Int,
    val day: String,
    val time: String,
    @SerialName("bus_assigned") val busAssigned: String? = null,
    @SerialName("schedule_set_id") val scheduleSetId: Int? = null,
    @SerialName("trip_from") val tripFrom: TransportLocation,
    @SerialName("trip_to") val tripTo: TransportLocation,
)
