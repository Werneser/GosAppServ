package gosuslugi.DTOs

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDTO(
    val userId: String,
    val fullName: String? = null,
    val passport: String? = null,
    val snils: String? = null,
    val phone: String? = null,
    val email: String? = null
)