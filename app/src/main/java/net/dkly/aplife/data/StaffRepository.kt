package net.dkly.aplife.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class StaffRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val mutex = Mutex()
    private var cached: List<Lecturer>? = null
    private var byCode: Map<String, Lecturer> = emptyMap()
    private var bySam: Map<String, Lecturer> = emptyMap()

    suspend fun all(): List<Lecturer> = ensureLoaded()

    suspend fun search(query: String, limit: Int = 50): List<Lecturer> {
        val q = query.trim()
        val list = ensureLoaded()
        if (q.isBlank()) return list.take(limit)
        val needle = q.lowercase()
        return list.asSequence()
            .filter {
                it.fullName.lowercase().contains(needle) ||
                    it.code?.lowercase()?.contains(needle) == true ||
                    it.id?.lowercase()?.contains(needle) == true ||
                    it.department?.lowercase()?.contains(needle) == true
            }
            .take(limit)
            .toList()
    }

    suspend fun byCode(code: String?): Lecturer? {
        if (code.isNullOrBlank()) return null
        ensureLoaded()
        return byCode[code.uppercase()]
    }

    suspend fun bySam(sam: String?): Lecturer? {
        if (sam.isNullOrBlank()) return null
        ensureLoaded()
        return bySam[sam.lowercase()]
    }

    private suspend fun ensureLoaded(): List<Lecturer> {
        cached?.let { return it }
        return mutex.withLock {
            cached?.let { return it }
            val list = withContext(Dispatchers.IO) {
                context.assets.open("staff-listing.json").bufferedReader().use { it.readText() }
            }.let { text -> json.decodeFromString<List<Lecturer>>(text) }
            cached = list
            byCode = list.asSequence()
                .mapNotNull { it.code?.takeIf(String::isNotBlank)?.let { c -> c.uppercase() to it } }
                .toMap()
            bySam = list.asSequence()
                .mapNotNull { it.id?.takeIf(String::isNotBlank)?.let { s -> s.lowercase() to it } }
                .toMap()
            list
        }
    }
}
