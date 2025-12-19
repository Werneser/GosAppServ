package gosuslugi.features.userinformation

import kotlinx.serialization.Serializable

@Serializable
data class UserInformationResponse(
    val success: Boolean,
    val message: String? = null,
    val user: UserResponse? = null // Измените тип на UserResponse
)


@Serializable
data class UserResponse(
    val userId: String,
    val phone: String,
    val name: String,
)
