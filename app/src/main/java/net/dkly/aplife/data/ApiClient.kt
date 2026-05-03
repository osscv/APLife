package net.dkly.aplife.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val TIMETABLE_URL =
        "https://s3-ap-southeast-1.amazonaws.com/open-ws/weektimetable"

    private const val EXAM_URL_BASE =
        "https://api.apiit.edu.my/examination/"

    private const val LECTURER_SCHEDULE_URL_BASE =
        "https://api.apiit.edu.my/lecturer-timetable/v2/"

    private const val QUIX_URL = "https://api.apiit.edu.my/quix/get/file"

    private const val TRANSIX_LOCATIONS_URL =
        "https://api.apiit.edu.my/transix-v2/locations"

    private const val TRANSIX_ACTIVE_SCHEDULE_URL =
        "https://api.apiit.edu.my/transix-v2/schedule/active"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun fetchTimetable(): List<TimetableEntry> = withContext(Dispatchers.IO) {
        val body = httpGet(TIMETABLE_URL)
        json.decodeFromString(body)
    }

    suspend fun fetchExams(intakeCode: String): List<ExamEntry> = withContext(Dispatchers.IO) {
        val body = httpGet(EXAM_URL_BASE + intakeCode.trim().uppercase())
        json.decodeFromString(body)
    }

    suspend fun fetchLecturerSchedule(samAccount: String): List<LecturerScheduleEntry> =
        withContext(Dispatchers.IO) {
            val body = httpGet(LECTURER_SCHEDULE_URL_BASE + samAccount.trim().lowercase())
            json.decodeFromString(body)
        }

    suspend fun fetchQuixCustomers(): List<QuixCompany> = withContext(Dispatchers.IO) {
        val body = httpGet(QUIX_URL, mapOf("X-Filename" to "quix-customers"))
        json.decodeFromString(body)
    }

    suspend fun fetchTransportLocations(): List<TransportLocation> = withContext(Dispatchers.IO) {
        val body = httpGet(TRANSIX_LOCATIONS_URL)
        json.decodeFromString(body)
    }

    suspend fun fetchActiveTransportSchedule(): TransportSchedule = withContext(Dispatchers.IO) {
        val body = httpGet(TRANSIX_ACTIVE_SCHEDULE_URL)
        json.decodeFromString(body)
    }

    private fun httpGet(urlString: String, headers: Map<String, String> = emptyMap()): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "APLife-Android/1.0")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("HTTP $code: $err")
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
