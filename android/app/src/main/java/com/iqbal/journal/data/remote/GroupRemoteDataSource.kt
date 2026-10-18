package com.iqbal.journal.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class GroupRemoteDataSource @Inject constructor(private val client: SupabaseClient) {
    /** RLS hanya mengembalikan grup yang saya ikuti (grup arsip tidak punya anggota). */
    suspend fun fetchMyGroups(): List<GroupDto> = client.from(Tables.GROUPS)
        .select(Columns.raw(GROUP_COLUMNS))
        .decodeList()

    suspend fun createGroup(name: String): CreateGroupResultDto = client.postgrest
        .rpc(Rpc.CREATE_GROUP, buildJsonObject { put("p_name", name) })
        .decodeList<CreateGroupResultDto>()
        .single()

    /** @return id grup yang digabungi. */
    suspend fun joinGroup(inviteCode: String): String = client.postgrest
        .rpc(Rpc.JOIN_GROUP, buildJsonObject { put("p_code", inviteCode) })
        .decodeAs()

    private companion object {
        const val GROUP_COLUMNS = "id, name, owner_id, invite_code, invite_expires_at, group_members(count)"
    }
}
