package com.iqbal.journal.data.repository

import com.iqbal.journal.data.local.WidgetSelectionDao
import com.iqbal.journal.data.toDomain
import com.iqbal.journal.data.toEntity
import com.iqbal.journal.domain.model.WidgetSelection
import com.iqbal.journal.domain.repository.WidgetRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WidgetRepositoryImpl @Inject constructor(private val dao: WidgetSelectionDao) : WidgetRepository {
    override val pinnedNoteIds: Flow<Set<String>> = dao.observePinnedNoteIds().map { it.toSet() }

    override suspend fun selection(appWidgetId: Int): WidgetSelection? = dao.get(appWidgetId)?.toDomain()

    override fun observeSelection(appWidgetId: Int): Flow<WidgetSelection?> =
        dao.observe(appWidgetId).map { it?.toDomain() }

    override suspend fun select(selection: WidgetSelection) = dao.upsert(selection.toEntity())

    override suspend fun remove(appWidgetIds: List<Int>) = dao.delete(appWidgetIds)
}
