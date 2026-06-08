package gosuslugi.features.userprofile

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileReceiveRemote(
    val fullName: String? = null,
    val passport: String? = null,
    val snils: String? = null,
    val phone: String? = null,
    val email: String? = null
)

@Serializable
data class UserProfileResponseRemote(
    val success: Boolean,
    val message: String? = null,
    val profile: gosuslugi.DTOs.UserProfileDTO? = null
)