package net.dkly.aplife.notes

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

class NoteRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "aplife_notes", Context.MODE_PRIVATE,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _state: MutableStateFlow<List<Note>> = MutableStateFlow(loadFromDisk())
    val notes: StateFlow<List<Note>> = _state.asStateFlow()

    fun all(): StateFlow<List<Note>> = notes

    fun forEvent(eventKey: String): kotlinx.coroutines.flow.Flow<List<Note>> =
        notes.map { list -> list.filter { it.eventKey == eventKey } }

    fun forModule(moduleId: String): kotlinx.coroutines.flow.Flow<List<Note>> =
        notes.map { list -> list.filter { it.moduleId == moduleId } }

    suspend fun insert(note: Note): Long {
        val toSave = if (note.id == 0L) note.copy(id = nextId()) else note
        _state.update { current ->
            (current.filterNot { it.id == toSave.id } + toSave)
                .sortedByDescending { it.dueAtMs ?: it.createdAtMs }
        }
        persist()
        return toSave.id
    }

    suspend fun update(note: Note) = insert(note).let {}

    suspend fun delete(note: Note) {
        _state.update { current -> current.filterNot { it.id == note.id } }
        persist()
    }

    private fun nextId(): Long {
        val existing = _state.value
        val current = existing.maxOfOrNull { it.id } ?: 0L
        return current + 1
    }

    private fun persist() {
        val text = json.encodeToString(_state.value)
        prefs.edit { putString(KEY_NOTES, text) }
    }

    private fun loadFromDisk(): List<Note> {
        val text = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Note>>(text) }.getOrDefault(emptyList())
    }

    private companion object {
        const val KEY_NOTES = "notes_json"
    }
}
