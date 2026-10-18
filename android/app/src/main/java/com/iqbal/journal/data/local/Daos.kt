package com.iqbal.journal.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MyProfileDao {
    @Query("SELECT * FROM my_profile LIMIT 1")
    fun observe(): Flow<MyProfileEntity?>

    @Upsert
    suspend fun upsert(profile: MyProfileEntity)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getById(id: String): GroupEntity?

    /** Samakan cache dengan server: grup yang tidak lagi diikuti ikut terhapus. */
    @Transaction
    suspend fun replaceAll(groups: List<GroupEntity>) {
        deleteAll()
        upsertAll(groups)
    }

    @Upsert
    suspend fun upsertAll(groups: List<GroupEntity>)

    @Query("DELETE FROM groups")
    suspend fun deleteAll()
}
