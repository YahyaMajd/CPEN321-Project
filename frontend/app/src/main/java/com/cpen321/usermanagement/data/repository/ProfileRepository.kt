package com.cpen321.usermanagement.data.repository

import android.net.Uri
import com.cpen321.usermanagement.data.remote.dto.User

interface ProfileRepository {
    suspend fun getProfile(): Result<User>
    suspend fun updateProfile(name: String, bio: String, profilePicture: String): Result<User>
    suspend fun deleteProfile(): Result<Unit>
    suspend fun uploadProfilePicture(pictureUri: Uri): Result<String>
    suspend fun updateMoverAvailability(availability: Map<String, List<List<String>>>): Result<User>
    suspend fun cashOut(): Result<User>
}