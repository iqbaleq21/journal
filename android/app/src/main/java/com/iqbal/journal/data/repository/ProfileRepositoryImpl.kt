package com.iqbal.journal.data.repository

import com.iqbal.journal.data.local.MyProfileDao
import com.iqbal.journal.data.remote.ProfileDto
import com.iqbal.journal.data.remote.ProfileRemoteDataSource
import com.iqbal.journal.data.remote.outcomeOf
import com.iqbal.journal.data.toDomain
import com.iqbal.journal.data.toEntity
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.validation.ProfileRules
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl @Inject constructor(
    private val remote: ProfileRemoteDataSource,
    private val dao: MyProfileDao
) : ProfileRepository {

    override val myProfile: Flow<Profile?> = dao.observe().map { it?.toDomain() }

    override val isSessionLost: Flow<Boolean> =
        combine(dao.observe(), remote.isSignedOut) { profile, signedOut -> profile != null && signedOut }

    override suspend fun saveProfile(displayName: String, email: String): Outcome<Profile> = outcomeOf {
        val profile = Profile(
            id = remote.ensureSignedIn(),
            displayName = ProfileRules.normalizeName(displayName),
            email = ProfileRules.normalizeEmail(email)
        )
        remote.upsertProfile(ProfileDto(profile.id, profile.displayName, profile.email))
        dao.replace(profile.toEntity())
        profile
    }
}
