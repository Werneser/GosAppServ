package gosuslugi.DTOs

import kotlinx.serialization.Serializable

@Serializable
data class UserInformationDTO(
    val userId: String,
    val userPhone: String,
    val userName: String,
)


