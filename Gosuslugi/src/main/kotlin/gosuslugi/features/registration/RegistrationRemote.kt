package gosuslugi.features.registration

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationReceiveRemote(
    val login: String,
    val phoneNumber: String,
    val password: String,
    val userName: String,
    val role: Int = 0
)

@Serializable
data class RegistrationResponseRemote(
    val token: String
)