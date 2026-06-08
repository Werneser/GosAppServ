// gosuslugi/features/userinformation/UserInformationModels.kt

package gosuslugi.features.userinformation

import kotlinx.serialization.Serializable

@Serializable
data class UserInformationResponse(
    val success: Boolean,
    val message: String? = null,
    val user: UserResponse? = null
)

@Serializable
data class UserResponse(
    val userId: String,
    val phone: String,
    val name: String,
    val role: Int = 0
)

@Serializable
data class LoginReceiveRemote(
    val login: String,
    val password: String
)

@Serializable
data class LoginResponseRemote(
    val token: String,
    val role: Int = 0
)