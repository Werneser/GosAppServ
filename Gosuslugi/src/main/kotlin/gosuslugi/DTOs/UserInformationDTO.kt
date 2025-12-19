package gosuslugi.DTOs

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentialsDTO(
    val userId: String,
    val login: String,
    val password: String,
    val token: String?
)

@Serializable
data class UserInformationDTO(
    val userId: String,
    val userPhone: String,
    val userName: String,
)


