package com.iqbal.journal.testing

import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.domain.model.WidgetSelection
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.repository.WidgetRepository
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeProfileRepository : ProfileRepository {
    override val myProfile = MutableStateFlow<Profile?>(null)
    var nextResult: Outcome<Profile>? = null
    val createCalls = mutableListOf<Pair<String, String>>()

    /** Bila diisi, createProfile menunggu sampai deferred ini selesai (untuk mengecek state "loading"). */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun createProfile(displayName: String, email: String): Outcome<Profile> {
        createCalls += displayName to email
        gate?.await()
        val result = nextResult ?: Outcome.Success(Profile("me", displayName, email))
        if (result is Outcome.Success) myProfile.value = result.value
        return result
    }
}

class FakeGroupRepository : GroupRepository {
    override val groups = MutableStateFlow<List<Group>>(emptyList())
    private val selected = MutableStateFlow<String?>(null)
    override val selectedGroupId: Flow<String?> = selected

    var refreshResult: Outcome<Unit> = Outcome.Success(Unit)
    var nextGroupResult: Outcome<Group>? = null
    val joinCalls = mutableListOf<String>()
    val createCalls = mutableListOf<String>()

    override suspend fun selectGroup(groupId: String) {
        selected.value = groupId
    }

    override suspend fun refresh(): Outcome<Unit> = refreshResult

    override suspend fun createGroup(name: String): Outcome<Group> {
        createCalls += name
        return nextGroupResult ?: Outcome.Success(group(name = name))
    }

    override suspend fun joinGroup(inviteCode: String): Outcome<Group> {
        joinCalls += inviteCode
        return nextGroupResult ?: Outcome.Success(group())
    }

    companion object {
        fun group(id: String = "g1", name: String = "Rumah") =
            Group(id, name, ownerId = "me", memberCount = 1, inviteCode = "K7QM4X", inviteExpiresAt = null)
    }
}

class FakeNoteRepository : NoteRepository {
    val notes = MutableStateFlow<List<Note>>(emptyList())
    var refreshResult: Outcome<Unit> = Outcome.Success(Unit)
    var writeFailure: Outcome.Failure? = null
    val refreshCalls = mutableListOf<String>()
    val createCalls = mutableListOf<Pair<String, String>>()
    val updateCalls = mutableListOf<Pair<String, String>>()
    val deleteCalls = mutableListOf<String>()

    override val changes: Flow<Unit> = notes.map { }

    override fun notes(groupId: String): Flow<List<Note>> = notes.map { list -> list.filter { it.groupId == groupId } }

    override fun note(noteId: String): Flow<Note?> = notes.map { list -> list.firstOrNull { it.id == noteId } }

    override suspend fun refresh(groupId: String): Outcome<Unit> {
        refreshCalls += groupId
        return refreshResult
    }

    override suspend fun create(groupId: String, content: String): Outcome<Note> {
        createCalls += groupId to content
        writeFailure?.let { return it }
        return Outcome.Success(note(id = "new", groupId = groupId, content = content))
    }

    override suspend fun update(noteId: String, content: String): Outcome<Note> {
        updateCalls += noteId to content
        writeFailure?.let { return it }
        return Outcome.Success(note(id = noteId, content = content))
    }

    override suspend fun delete(noteId: String): Outcome<Unit> {
        deleteCalls += noteId
        writeFailure?.let { return it }
        notes.value = notes.value.filterNot { it.id == noteId }
        return Outcome.Success(Unit)
    }

    override suspend fun listenForChanges(onSubscribed: suspend () -> Unit): Outcome<Unit> {
        onSubscribed()
        return Outcome.Success(Unit)
    }

    companion object {
        fun note(id: String = "n1", groupId: String = "g1", authorId: String = "me", content: String = "Galon habis") =
            Note(
                id = id,
                groupId = groupId,
                authorId = authorId,
                authorName = "Ayah",
                content = content,
                color = NoteColor.Plain,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH
            )
    }
}

class FakeWidgetRepository : WidgetRepository {
    val selections = MutableStateFlow<Map<Int, WidgetSelection>>(emptyMap())

    override val pinnedNoteIds: Flow<Set<String>> = selections.map { map -> map.values.map { it.noteId }.toSet() }

    override suspend fun selection(appWidgetId: Int): WidgetSelection? = selections.value[appWidgetId]

    override fun observeSelection(appWidgetId: Int): Flow<WidgetSelection?> = selections.map { it[appWidgetId] }

    override suspend fun select(selection: WidgetSelection) {
        selections.value = selections.value + (selection.appWidgetId to selection)
    }

    override suspend fun remove(appWidgetIds: List<Int>) {
        selections.value = selections.value - appWidgetIds.toSet()
    }
}
