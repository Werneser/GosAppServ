package gosuslugi.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import gosuslugi.database.Appointments
import gosuslugi.DTOs.AppointmentDTO
import gosuslugi.DTOs.ServiceDTO
import gosuslugi.features.appointment.ServiceReceiveRemote
import java.time.Instant
import java.util.*

class AppointmentInformationRepository {
    private val logger = LoggerFactory.getLogger(AppointmentInformationRepository::class.java)

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
                it[Appointments.formData] = formData.toString()
            }
            logger.info("Appointment created for user: $user with service: ${service.id}")
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
    }

    private fun toAppointmentDTO(row: ResultRow): AppointmentDTO {
        return AppointmentDTO(
            id = row[Appointments.id],
            user = row[Appointments.user],
            service = ServiceDTO(
                id = row[Appointments.serviceId],
                title = row[Appointments.serviceTitle],
                description = row[Appointments.serviceDescription],
                category = row[Appointments.serviceCategory],
                requiredFields = emptyList() // Если нужно, можно добавить логику для получения requiredFields
            ),
            appliedAt = row[Appointments.appliedAt],
            status = row[Appointments.status],
            formData = row[Appointments.formData]?.let { parseFormData(it) } ?: emptyMap()
        )
    }

    private fun parseFormData(formDataString: String): Map<String, String> {
        // Пример парсинга строки в Map<String, String>
        return emptyMap() // Реализуйте парсинг в зависимости от формата хранения
    }
}

enum class AppointmentStatus {
    SUBMITTED,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    NEEDS_INFO
}
