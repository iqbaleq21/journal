package com.iqbal.journal.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class GroupRemoteDataSource @Inject constructor(private val client: SupabaseClient) {
    /** RLS hanya mengembalikan grup yang saya ikuti (grup arsip tidak punya anggota). */
    suspend fun fetchMyGroups(): List<GroupDto> = client.withSession().from(Tables.GROUPS)
        .select(Columns.raw(GROUP_COLUMNS))
        .decodeList()

    suspend fun createGroup(name: String): CreateGroupResultDto = client.withSession().postgrest
        .rpc(Rpc.CREATE_GROUP, buildJsonObject { put("p_name", name) })
        .decodeList<CreateGroupResultDto>()
        .single()

    /** @return id grup yang digabungi. */
    suspend fun joinGroup(inviteCode: String): String = client.withSession().postgrest
        .rpc(Rpc.JOIN_GROUP, buildJsonObject { put("p_code", inviteCode) })
        .decodeAs()

    /** Keanggotaan di semua grup yang saya ikuti; RLS hanya mengembalikan baris grup tersebut. */
    suspend fun fetchMemberships(): List<MembershipDto> = client.withSession().from(Tables.GROUP_MEMBERS)
        .select(Columns.list("group_id", "user_id", "joined_at", "notify"))
        .decodeList()

    suspend fun leaveGroup(groupId: String) {
        client.withSession().postgrest.rpc(Rpc.LEAVE_GROUP, buildJsonObject { put("p_group_id", groupId) })
    }

    suspend fun removeMember(groupId: String, userId: String) {
        client.withSession().postgrest.rpc(
            Rpc.REMOVE_MEMBER,
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_user_id", userId)
            }
        )
    }

    suspend fun renameGroup(groupId: String, name: String) {
        client.withSession().postgrest.rpc(
            Rpc.RENAME_GROUP,
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_name", name)
            }
        )
    }

    suspend fun regenerateInvite(groupId: String) {
        client.withSession().postgrest.rpc(Rpc.REGENERATE_INVITE, buildJsonObject { put("p_group_id", groupId) })
    }

    /** RLS hanya mengizinkan mengubah kolom notify milik sendiri. */
    suspend fun setNotify(groupId: String, notify: Boolean) {
        client.withSession().from(Tables.GROUP_MEMBERS).update({ set("notify", notify) }) {
            filter {
                eq("group_id", groupId)
                eq("user_id", currentUserId())
            }
        }
    }

    fun currentUserId(): String = client.auth.currentUserOrNull()?.id ?: throw NoSessionException()

    private companion object {
        const val GROUP_COLUMNS = "id, name, owner_id, invite_code, invite_expires_at, group_members(count)"
    }
}
