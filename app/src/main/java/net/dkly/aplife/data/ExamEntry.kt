package net.dkly.aplife.data

import kotlinx.serialization.Serializable

@Serializable
data class ExamEntry(
    val intake: String,
    val module: String,
    val subjectDescription: String,
    val assessmentType: String? = null,
    val examType: String? = null,
    val since: String,
    val until: String,
    val endDate: String? = null,
    val venue: String? = null,
    val resultDate: String? = null,
    val questionReleaseDate: String? = null,
    val appraisalsDue: String? = null,
    val docketsDue: String? = null,
)
