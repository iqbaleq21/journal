package com.iqbal.journal.testing

import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Member
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.model.NoteDraft
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.domain.model.ThemeMode
import com.iqbal.journal.domain.model.WidgetSelection
import com.iqbal.journal.domain.repository.AppearanceRepository
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.repository.WidgetRepository
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FakeProfileRepository : ProfileRepository {
    override val myProfile = MutableStateFlow<Profile?>(null)
    var nextResult: Outcome<Profile>? = null
    val createCalls = mutableListOf<Pair<String, String>>()

    /** Bila diisi, saveProfile menunggu sampai deferred ini selesai (untuk mengecek state "loading"). */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun saveProfile(displayName: String, email: String): Outcome<Profile> {
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

    /** Anggota per grup; aksi grup di bawah mengubahnya seperti server. */
    val membersByGroup = MutableStateFlow<Map<String, List<Member>>>(emptyMap())
    var actionResult: Outcome<Unit> = Outcome.Success(Unit)
    val actions = mutableListOf<String>()

    override fun members(groupId: String): Flow<List<Member>> = membersByGroup.map { it[groupId].orEmpty() }

    override suspend fun leaveGroup(groupId: String): Outcome<Unit> = act("leave $groupId") {
        groups.value = groups.value.filterNot { it.id == groupId }
    }

    override suspend fun removeMember(groupId: String, userId: String): Outcome<Unit> = act("remove $groupId $userId") {
        membersByGroup.value =
            membersByGroup.value + (groupId to members(groupId).first().filterNot { it.userId == userId })
    }

    override suspend fun renameGroup(groupId: String, name: String): Outcome<Unit> = act("rename $groupId $name") {
        groups.value = groups.value.map { if (it.id == groupId) it.copy(name = name.trim()) else it }
    }

    override suspend fun regenerateInvite(groupId: String): Outcome<Unit> = act("regenerate $groupId") {}

    override suspend fun setNotify(groupId: String, enabled: Boolean): Outcome<Unit> = act("notify $groupId $enabled") {
        groups.value = groups.value.map { if (it.id == groupId) it.copy(notify = enabled) else it }
    }

    private suspend fun act(name: String, onSuccess: suspend () -> Unit): Outcome<Unit> {
        actions += name
        if (actionResult is Outcome.Success) onSuccess()
        return actionResult
    }

    companion object {
        fun group(id: String = "g1", name: String = "Rumah", ownerId: String = "me") =
            Group(id, name, ownerId = ownerId, memberCount = 1, inviteCode = "K7QM4X", inviteExpiresAt = null)

        fun member(userId: String, name: String, groupId: String = "g1") =
            Member(groupId = groupId, userId = userId, displayName = name, joinedAt = Instant.EPOCH)
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

    /** Isi server tiruan untuk syncNote; catatan yang tidak ada di sini dianggap sudah dihapus. */
    val serverNotes = mutableMapOf<String, Note>()
    var syncFailure: Outcome.Failure? = null

    override suspend fun syncNote(noteId: String): Outcome<Note?> {
        syncFailure?.let { return it }
        val note = serverNotes[noteId]
        notes.value = notes.value.filterNot { it.id == noteId } + listOfNotNull(note)
        return Outcome.Success(note)
    }

    override suspend fun trimCache() = Unit

    override suspend fun flushPending(): Boolean = true

    /** Isian yang dikirim pada create/update terakhir. */
    var lastDraft: NoteDraft? = null
    val lastColor: NoteColor? get() = lastDraft?.color

    override suspend fun create(groupId: String, draft: NoteDraft): Outcome<Note> {
        createCalls += groupId to draft.content
        lastDraft = draft
        writeFailure?.let { return it }
        return Outcome.Success(note(id = "new", groupId = groupId, content = draft.content))
    }

    override suspend fun update(noteId: String, draft: NoteDraft): Outcome<Note> {
        updateCalls += noteId to draft.content
        lastDraft = draft
        writeFailure?.let { return it }
        return Outcome.Success(note(id = noteId, content = draft.content))
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
        fun note(
            id: String = "n1",
            groupId: String = "g1",
            authorId: String = "me",
            content: String = "Galon habis",
            shared: Boolean = false
        ) = Note(
            id = id,
            groupId = groupId,
            authorId = authorId,
            authorName = "Ayah",
            content = content,
            color = NoteColor.Plain,
            shared = shared,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )
    }
}

class FakeWidgetRepository : WidgetRepository {
    val selections = MutableStateFlow<Map<Int, WidgetSelection>>(emptyMap())

    override val pinnedNoteIds: Flow<Set<String>> =
        selections.map { map -> map.values.mapNotNull { it.noteId }.toSet() }

    override suspend fun selection(appWidgetId: Int): WidgetSelection? = selections.value[appWidgetId]

    override fun observeSelection(appWidgetId: Int): Flow<WidgetSelection?> = selections.map { it[appWidgetId] }

    override suspend fun select(selection: WidgetSelection) {
        selections.value = selections.value + (selection.appWidgetId to selection)
    }

    override suspend fun remove(appWidgetIds: List<Int>) {
        selections.value = selections.value - appWidgetIds.toSet()
    }
}

class FakeAppearanceRepository : AppearanceRepository {
    override val themeMode = MutableStateFlow(ThemeMode.System)

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}
