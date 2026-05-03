package net.dkly.aplife.notes

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: Long = 0,
    val type: NoteType,
    val title: String,
    val body: String = "",
    /** Optional CLASS_CODE this note is attached to. */
    val eventKey: String? = null,
    /** Optional module id for grouping notes by module. */
    val moduleId: String? = null,
    /** Optional lecturer code (LECTID). */
    val lecturerCode: String? = null,
    /** Optional lecturer SAMACCOUNTNAME. */
    val lecturerSam: String? = null,
    /** When the note "happens" (due date / appointment time). */
    val dueAtMs: Long? = null,
    val endAtMs: Long? = null,
    /** Optional calendar event id this note is linked to. */
    val calendarEventId: Long? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
)
