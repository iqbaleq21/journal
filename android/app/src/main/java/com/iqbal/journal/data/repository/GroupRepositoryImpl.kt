package com.iqbal.journal.data.repository

import com.iqbal.journal.data.local.GroupDao
import com.iqbal.journal.data.local.UserPreferences
import com.iqbal.journal.data.remote.GroupRemoteDataSource
import com.iqbal.journal.data.remote.outcomeOf
import com.iqbal.journal.data.toDomain
import com.iqbal.journal.data.toEntity
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.validation.GroupRules
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GroupRepositoryImpl @Inject constructor(
    private val remote: GroupRemoteDataSource,
    private val dao: GroupDao,
    private val preferences: UserPreferences
) : GroupRepository {

    override val groups: Flow<List<Group>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override val selectedGroupId: Flow<String?> =
        combine(preferences.selectedGroupId, groups) { selected, groups ->
            selected?.takeIf { id -> groups.any { it.id == id } } ?: groups.firstOrNull()?.id
        }

    override suspend fun selectGroup(groupId: String) = preferences.setSelectedGroupId(groupId)

    override suspend fun refresh(): Outcome<Unit> = outcomeOf { syncGroups() }

    override suspend fun createGroup(name: String): Outcome<Group> = outcomeOf {
        val created = remote.createGroup(GroupRules.normalizeName(name))
        syncAndSelect(created.groupId)
    }

    override suspend fun joinGroup(inviteCode: String): Outcome<Group> = outcomeOf {
        val groupId = remote.joinGroup(GroupRules.normalizeInviteCode(inviteCode))
        syncAndSelect(groupId)
    }

    private suspend fun syncGroups() {
        dao.replaceAll(remote.fetchMyGroups().map { it.toEntity() })
    }

    private suspend fun syncAndSelect(groupId: String): Group {
        syncGroups()
        preferences.setSelectedGroupId(groupId)
        return checkNotNull(dao.getById(groupId)) { "Grup $groupId tidak ada setelah sinkron" }.toDomain()
    }
}
