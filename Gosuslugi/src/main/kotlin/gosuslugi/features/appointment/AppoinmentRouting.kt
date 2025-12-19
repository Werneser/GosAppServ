package gosuslugi.features.appointments

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import gosuslugi.repository.AppointmentInformationRepository
import gosuslugi.features.appointment.AppointmentReceiveRemote
import gosuslugi.features.appointment.AppointmentResponseRemote
import gosuslugi.repository.AppointmentStatus

val appointmentLogger = LoggerFactory.getLogger("AppointmentRouteLogger")

fun Application.configureAppointmentRouting() {
    val appointmentRepository = AppointmentInformationRepository()

    routing {
        post("/appointments") {
            val receive = try {
                call.receive<AppointmentReceiveRemote>()
            } catch (e: Exception) {
                appointmentLogger.error("Appointment creation error: ${e.message}")
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid request format")
            }

            val appointment = appointmentRepository.createAppointment(
                user = receive.user,
                service = receive.service,
                formData = receive.formData
            )

            appointmentLogger.info("Appointment created for user: ${receive.user}")
            call.respond(
                HttpStatusCode.Created,
                AppointmentResponseRemote(
                    message = "Appointment created successfully",
                    appointment = appointment
                )
            )
        }

        get("/appointments/user/{username}") {
            val username = call.parameters["username"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Missing username"
            )
            val appointments = appointmentRepository.getAppointmentsByUser(username)
            call.respond(HttpStatusCode.OK, appointments)
        }

        patch("/appointments/{id}/status") {
            val id = call.parameters["id"] ?: return@patch call.respond(
                HttpStatusCode.BadRequest,
                "Missing appointment ID"
            )
            val status = try {
                val statusIndex = call.request.queryParameters["status"]?.toIntOrNull()
                AppointmentStatus.values().find { it.ordinal == statusIndex }
            } catch (e: Exception) {
                return@patch call.respond(HttpStatusCode.BadRequest, "Invalid status")
            } ?: return@patch call.respond(HttpStatusCode.BadRequest, "Missing status")

            appointmentRepository.updateAppointmentStatus(id, status)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Status updated successfully"))
        }
    }
}
