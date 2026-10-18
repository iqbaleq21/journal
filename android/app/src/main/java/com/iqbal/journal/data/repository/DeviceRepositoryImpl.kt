package com.iqbal.journal.data.repository

import com.iqbal.journal.data.push.PushTokenSource
import com.iqbal.journal.data.remote.DeviceRemoteDataSource
import com.iqbal.journal.data.remote.outcomeOf
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.DeviceRepository
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val tokenSource: PushTokenSource,
    private val remote: DeviceRemoteDataSource
) : DeviceRepository {
    override suspend fun registerThisDevice(): Outcome<Unit> = outcomeOf { remote.upsert(tokenSource.currentToken()) }

    override suspend fun register(token: String): Outcome<Unit> = outcomeOf { remote.upsert(token) }
}
