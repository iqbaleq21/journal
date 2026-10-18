package com.iqbal.journal.testing

import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

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
