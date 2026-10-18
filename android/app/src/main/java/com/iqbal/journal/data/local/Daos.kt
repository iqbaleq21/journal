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

    /** Samakan cache dengan server: grup yang tidak lagi diikuti, beserta catatannya, ikut terhapus. */
    @Transaction
    suspend fun replaceAll(groups: List<GroupEntity>) {
        deleteAllExcept(groups.map { it.id })
        upsertAll(groups)
        deleteNotesOfLeftGroups()
    }

    @Upsert
    suspend fun upsertAll(groups: List<GroupEntity>)

    @Query("DELETE FROM groups WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)

    // Tanpa foreign key: catatan dari Realtime bisa tiba sebelum grupnya tersinkron.
    @Query("DELETE FROM notes WHERE group_id NOT IN (SELECT id FROM groups)")
    suspend fun deleteNotesOfLeftGroups()
}

@Dao
interface NoteDao {
    @Query(
        """
        SELECT notes.*, authors.display_name AS author_name FROM notes
        LEFT JOIN authors ON authors.id = notes.author_id
        WHERE notes.group_id = :groupId
        ORDER BY notes.updated_at DESC
        """
    )
    fun observeByGroup(groupId: String): Flow<List<NoteWithAuthor>>

    @Query(
        """
        SELECT notes.*, authors.display_name AS author_name FROM notes
        LEFT JOIN authors ON authors.id = notes.author_id
        WHERE notes.id = :id
        """
    )
    fun observeById(id: String): Flow<NoteWithAuthor?>

    /** Ringkasan isi tabel yang berubah setiap ada tambah, ubah, atau hapus. */
    // Jumlah penulis ikut dihitung agar nama yang baru terambil juga memicu pembaruan.
    @Query(
        """
        SELECT COUNT(*) || ':' || IFNULL(MAX(updated_at), 0) || ':' || IFNULL(SUM(updated_at), 0)
        || ':' || (SELECT COUNT(*) FROM authors) FROM notes
        """
    )
    fun observeVersion(): Flow<String>

    /** Samakan catatan sebuah grup dengan hasil terbaru dari server; catatan yang dipakai widget dipertahankan. */
    @Transaction
    suspend fun replaceForGroup(groupId: String, notes: List<NoteEntity>) {
        deleteFromGroupExcept(groupId, notes.map { it.id })
        upsertAll(notes)
    }

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Upsert
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        DELETE FROM notes WHERE group_id = :groupId AND id NOT IN (:keepIds)
        AND id NOT IN (SELECT note_id FROM widget_selections)
        """
    )
    suspend fun deleteFromGroupExcept(groupId: String, keepIds: List<String>)
}

@Dao
interface WidgetSelectionDao {
    @Query("SELECT DISTINCT note_id FROM widget_selections")
    fun observePinnedNoteIds(): Flow<List<String>>

    @Query("SELECT note_id FROM widget_selections WHERE group_id = :groupId")
    suspend fun pinnedNoteIds(groupId: String): List<String>

    @Query("SELECT * FROM widget_selections WHERE app_widget_id = :appWidgetId")
    suspend fun get(appWidgetId: Int): WidgetSelectionEntity?

    @Query("SELECT * FROM widget_selections WHERE app_widget_id = :appWidgetId")
    fun observe(appWidgetId: Int): Flow<WidgetSelectionEntity?>

    @Upsert
    suspend fun upsert(selection: WidgetSelectionEntity)

    @Query("DELETE FROM widget_selections WHERE app_widget_id IN (:appWidgetIds)")
    suspend fun delete(appWidgetIds: List<Int>)
}

@Dao
interface AuthorDao {
    @Upsert
    suspend fun upsertAll(authors: List<AuthorEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM authors WHERE id = :id)")
    suspend fun exists(id: String): Boolean
}
