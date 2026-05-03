package net.dkly.aplife.data

import kotlinx.serialization.Serializable

/**
 * A user-applied override for a single class entry.
 * `classCode` (CLASS_CODE) is the stable key. Any unset field falls back to the
 * value from the upstream timetable.
 */
@Serializable
data class ClassOverride(
    val classCode: String,
    val timeFromIso: String? = null,
    val timeToIso: String? = null,
    val room: String? = null,
    val lecturerCode: String? = null,
    val lecturerSam: String? = null,
    val lecturerName: String? = null,
)
