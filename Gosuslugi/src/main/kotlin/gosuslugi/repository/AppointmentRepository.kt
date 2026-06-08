package gosuslugi.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import gosuslugi.database.Appointments
import gosuslugi.DTOs.AppointmentDTO
import gosuslugi.DTOs.ServiceDTO
import gosuslugi.features.appointment.ServiceReceiveRemote
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.time.Instant
import java.util.*

class AppointmentInformationRepository {
    private val logger = LoggerFactory.getLogger(AppointmentInformationRepository::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun getAppointmentsByUser(user: String): List<AppointmentDTO> = transaction {
        Appointments.select { Appointments.user eq user }
            .map { toAppointmentDTO(it) }
    }

    fun createAppointment(
        user: String,
        service: ServiceReceiveRemote,
        formData: Map<String, String>
    ): AppointmentDTO {
        val appointmentId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        // Сериализуем formData в JSON
        val formDataJson = try {
            json.encodeToString(formData)
        } catch (e: Exception) {
            logger.error("Failed to serialize formData: ${e.message}")
            "{}"
        }

        transaction {
            Appointments.insert {
                it[id] = appointmentId
                it[Appointments.user] = user
                it[Appointments.serviceId] = service.id
                it[Appointments.serviceTitle] = service.title
                it[Appointments.serviceDescription] = service.description
                it[Appointments.serviceCategory] = service.category
                it[Appointments.appliedAt] = now
                it[Appointments.status] = AppointmentStatus.SUBMITTED.ordinal
                it[Appointments.formData] = formDataJson  // Сохраняем как JSON строку
            }
            logger.info("Appointment created for user: $user with service: ${service.id}")
            logger.info("FormData saved: $formDataJson")
        }

        return getAppointmentById(appointmentId)!!
    }

    fun getAppointmentById(id: String): AppointmentDTO? = transaction {
        Appointments.select { Appointments.id eq id }
            .map { toAppointmentDTO(it) }
            .singleOrNull()
    }

    fun updateAppointmentStatus(id: String, status: AppointmentStatus) = transaction {
        Appointments.update({ Appointments.id eq id }) {
            it[Appointments.status] = status.ordinal
        }
        logger.info("Appointment $id status updated to: $status")
    }

    private fun toAppointmentDTO(row: ResultRow): AppointmentDTO {
        val formDataString = row[Appointments.formData]
        val formData = parseFormData(formDataString)

        logger.debug("Parsing formData: $formDataString -> $formData")

        return AppointmentDTO(
            id = row[Appointments.id],
            user = row[Appointments.user],
            service = ServiceDTO(
                id = row[Appointments.serviceId],
                title = row[Appointments.serviceTitle],
                description = row[Appointments.serviceDescription],
                category = row[Appointments.serviceCategory],
                requiredFields = emptyList()
            ),
            appliedAt = row[Appointments.appliedAt],
            status = row[Appointments.status],
            formData = formData
        )
    }

    private fun parseFormData(formDataString: String?): Map<String, String> {
        if (formDataString.isNullOrEmpty() || formDataString == "null") {
            return emptyMap()
        }

        return try {
            // Пробуем распарсить как JSON
            json.decodeFromString<Map<String, String>>(formDataString)
        } catch (e: Exception) {
            logger.warn("Failed to parse formData as JSON: $formDataString, error: ${e.message}")

            try {
                parseLegacyFormData(formDataString)
            } catch (e2: Exception) {
                logger.error("Failed to parse legacy formData: $formDataString")
                emptyMap()
            }
        }
    }

    private fun parseLegacyFormData(formDataString: String): Map<String, String> {
        val content = formDataString.trim().removeSurrounding("{", "}")
        if (content.isEmpty()) return emptyMap()

        return content.split(",").mapNotNull { pair ->
            val parts = pair.split("=")
            if (parts.size == 2) {
                parts[0].trim() to parts[1].trim()
            } else {
                null
            }
        }.toMap()
    }

    fun getAllAppointments(): List<AppointmentDTO> = transaction {
        Appointments.selectAll()
            .map { toAppointmentDTO(it) }
    }
}

enum class AppointmentStatus {
    SUBMITTED,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    NEEDS_INFO
}