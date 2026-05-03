package net.dkly.aplife.data

class TimetableRepository {

    @Volatile
    private var cache: List<TimetableEntry>? = null

    suspend fun loadAll(forceRefresh: Boolean = false): List<TimetableEntry> {
        val current = cache
        if (!forceRefresh && current != null) return current
        val fresh = ApiClient.fetchTimetable()
        cache = fresh
        return fresh
    }

    suspend fun loadForIntake(
        intakeCode: String,
        forceRefresh: Boolean = false,
    ): List<TimetableEntry> {
        val target = intakeCode.trim().uppercase()
        return loadAll(forceRefresh).filter { it.intake.equals(target, ignoreCase = true) }
    }

    fun availableGroups(entries: List<TimetableEntry>): List<String> =
        entries.mapNotNull { it.grouping?.takeIf(String::isNotBlank) }
            .distinct()
            .sorted()

    suspend fun fetchExams(intakeCode: String): List<ExamEntry> =
        ApiClient.fetchExams(intakeCode)

    suspend fun distinctIntakes(forceRefresh: Boolean = false): List<String> =
        loadAll(forceRefresh)
            .asSequence()
            .map { it.intake.uppercase() }
            .distinct()
            .sorted()
            .toList()
}
