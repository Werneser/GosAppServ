package gosuslugi.features.appointment

import gosuslugi.DTOs.AppointmentDTO
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentReceiveRemote(
    val user: String,
    val service: ServiceReceiveRemote,
    val formData: Map<String, String> = emptyMap()
)

@Serializable
data class ServiceReceiveRemote(
    val id: String,
    val title: String,
    val description: String,
    val category: Int,
    val requiredFields: List<String>
)

@Serializable
data class AppointmentResponseRemote(
    val message: String,
    val appointment: AppointmentDTO? = null
)
