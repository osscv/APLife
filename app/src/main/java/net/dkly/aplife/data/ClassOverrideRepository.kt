package net.dkly.aplife.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

class ClassOverrideRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "aplife_class_overrides", Context.MODE_PRIVATE,
    )
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _state: MutableStateFlow<Map<String, ClassOverride>> =
        MutableStateFlow(loadFromDisk())
    val overrides: StateFlow<Map<String, ClassOverride>> = _state.asStateFlow()

    fun put(override: ClassOverride) {
        if (override.classCode.isBlank()) return
        _state.update { it + (override.classCode to override) }
        persist()
    }

    fun clear(classCode: String) {
        _state.update { it - classCode }
        persist()
    }

    fun apply(entry: TimetableEntry): TimetableEntry {
        val key = entry.classCode ?: return entry
        val o = _state.value[key] ?: return entry
        return entry.copy(
            timeFromIso = o.timeFromIso ?: entry.timeFromIso,
            timeToIso = o.timeToIso ?: entry.timeToIso,
            room = o.room ?: entry.room,
            lecturerId = o.lecturerCode ?: entry.lecturerId,
            lecturerAccount = o.lecturerSam ?: entry.lecturerAccount,
            lecturerName = o.lecturerName ?: entry.lecturerName,
        )
    }

    private fun persist() {
        val text = json.encodeToString(_state.value.values.toList())
        prefs.edit { putString(KEY, text) }
    }

    private fun loadFromDisk(): Map<String, ClassOverride> {
        val text = prefs.getString(KEY, null) ?: return emptyMap()
        val list = runCatching { json.decodeFromString<List<ClassOverride>>(text) }
            .getOrDefault(emptyList())
        return list.associateBy { it.classCode }
    }

    private companion object {
        const val KEY = "overrides_json"
    }
}
