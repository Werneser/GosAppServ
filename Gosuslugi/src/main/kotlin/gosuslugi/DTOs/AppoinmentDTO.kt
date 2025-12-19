package gosuslugi.DTOs

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AppointmentDTO(
    val id: String,
    val user: String,
    val service: ServiceDTO,
    val appliedAt: String,
    val status: Int,
    val formData: Map<String, String> = emptyMap()
)

@Serializable
data class ServiceDTO(
    val id: String,
    val title: String,
    val description: String,
    val category: Int,
    val requiredFields: List<String>
)
