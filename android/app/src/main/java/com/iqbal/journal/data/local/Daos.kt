package com.iqbal.journal.data.local

import androidx.room.Dao
import androidx.room.Insert
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

    @Query("DELETE FROM my_profile")
    suspend fun deleteAll()

    /** Selalu satu baris: masuk lagi setelah sesi hilang memberi id pengguna baru. */
    @Transaction
    suspend fun replace(profile: MyProfileEntity) {
        deleteAll()
        upsert(profile)
    }
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getById(id: String): GroupEntity?

    @Query("UPDATE groups SET notify = :notify WHERE id = :id")
    suspend fun setNotify(id: String, notify: Boolean)

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
interface MemberDao {
    @Query(
        """
        SELECT members.*, authors.display_name AS display_name FROM members
        LEFT JOIN authors ON authors.id = members.user_id
        WHERE members.group_id = :groupId
        ORDER BY members.joined_at
        """
    )
    fun observeByGroup(groupId: String): Flow<List<MemberWithName>>

    @Transaction
    suspend fun replaceAll(members: List<MemberEntity>) {
        deleteAll()
        upsertAll(members)
    }

    @Upsert
    suspend fun upsertAll(members: List<MemberEntity>)

    @Query("DELETE FROM members")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {
    @Query(
        """
        SELECT notes.*, authors.display_name AS author_name,
        EXISTS(SELECT 1 FROM pending_ops WHERE pending_ops.note_id = notes.id) AS is_pending FROM notes
        LEFT JOIN authors ON authors.id = notes.author_id
        WHERE notes.group_id = :groupId
        ORDER BY notes.updated_at DESC
        """
    )
    fun observeByGroup(groupId: String): Flow<List<NoteWithAuthor>>

    @Query(
        """
        SELECT notes.*, authors.display_name AS author_name,
        EXISTS(SELECT 1 FROM pending_ops WHERE pending_ops.note_id = notes.id) AS is_pending FROM notes
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

    /**
     * Samakan catatan sebuah grup dengan hasil terbaru dari server; catatan yang dipakai widget dipertahankan.
     * Subquery widget_selections selalu menyaring NULL: `x NOT IN (..., NULL)` bernilai NULL,
     * sehingga tanpa saringan itu tidak ada yang terhapus.
     */
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

    // Hitungan berkorelasi, bukan window function: SQLite di Android 8 belum mendukungnya.
    @Query(
        """
        DELETE FROM notes WHERE id NOT IN (SELECT note_id FROM widget_selections WHERE note_id IS NOT NULL)
        AND id NOT IN (SELECT note_id FROM pending_ops)
        AND (SELECT COUNT(*) FROM notes newer
             WHERE newer.group_id = notes.group_id AND newer.updated_at > notes.updated_at) >= :limitPerGroup
        """
    )
    suspend fun deleteBeyondLimit(limitPerGroup: Long)

    @Query(
        """
        DELETE FROM notes WHERE group_id = :groupId AND id NOT IN (:keepIds)
        AND id NOT IN (SELECT note_id FROM widget_selections WHERE note_id IS NOT NULL)
        AND id NOT IN (SELECT note_id FROM pending_ops)
        """
    )
    suspend fun deleteFromGroupExcept(groupId: String, keepIds: List<String>)
}

@Dao
interface WidgetSelectionDao {
    // Widget mode "terbaru" (note_id NULL) tidak menahan catatan tertentu.
    @Query("SELECT DISTINCT note_id FROM widget_selections WHERE note_id IS NOT NULL")
    fun observePinnedNoteIds(): Flow<List<String>>

    @Query("SELECT note_id FROM widget_selections WHERE group_id = :groupId AND note_id IS NOT NULL")
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
interface PendingOpDao {
    @Insert
    suspend fun insert(op: PendingOpEntity)

    @Query("SELECT * FROM pending_ops ORDER BY id")
    suspend fun getAll(): List<PendingOpEntity>

    @Query("SELECT DISTINCT note_id FROM pending_ops")
    suspend fun pendingNoteIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM pending_ops WHERE note_id = :noteId)")
    suspend fun hasPending(noteId: String): Boolean

    @Query("DELETE FROM pending_ops WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AuthorDao {
    @Upsert
    suspend fun upsertAll(authors: List<AuthorEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM authors WHERE id = :id)")
    suspend fun exists(id: String): Boolean
}
