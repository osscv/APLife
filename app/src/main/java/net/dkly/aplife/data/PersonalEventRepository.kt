package net.dkly.aplife.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

class PersonalEventRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "aplife_personal_events", Context.MODE_PRIVATE,
    )
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _state: MutableStateFlow<List<PersonalEvent>> =
        MutableStateFlow(loadFromDisk())
    val events: StateFlow<List<PersonalEvent>> = _state.asStateFlow()

    fun upsert(event: PersonalEvent): Long {
        val toSave = if (event.id == 0L) event.copy(id = nextId()) else event
        _state.update { current ->
            (current.filterNot { it.id == toSave.id } + toSave)
                .sortedBy { it.startMs }
        }
        persist()
        return toSave.id
    }

    fun delete(event: PersonalEvent) {
        _state.update { it.filterNot { e -> e.id == event.id } }
        persist()
    }

    private fun nextId(): Long {
        val max = _state.value.maxOfOrNull { it.id } ?: 0L
        return max + 1
    }

    private fun persist() {
        val text = json.encodeToString(_state.value)
        prefs.edit { putString(KEY, text) }
    }

    private fun loadFromDisk(): List<PersonalEvent> {
        val text = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<PersonalEvent>>(text) }
            .getOrDefault(emptyList())
    }

    private companion object { const val KEY = "events_json" }
}
