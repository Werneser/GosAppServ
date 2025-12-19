package gosuslugi.features.registration

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import gosuslugi.repository.UserInformationRepository
import gosuslugi.utils.isValidPhoneNumber

val registrationLogger = LoggerFactory.getLogger("RegistrationRouteLogger")

fun Application.configureRegistrationRouting() {
    val userInformationRepository = UserInformationRepository()

    routing {
        post("/register") {
            val receive = try {
                call.receive<RegistrationReceiveRemote>()
            } catch (e: Exception) {
                registrationLogger.error("Registration error: ${e.message}")
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Invalid request format"
                )
            }

            if (!receive.phoneNumber.isValidPhoneNumber()) {
                registrationLogger.warn("Invalid phone format: ${receive.phoneNumber}")
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Phone is not valid"
                )
            }

            if (userInformationRepository.getUserByPhone(receive.phoneNumber) != null) {
                registrationLogger.warn("User already exists: ${receive.phoneNumber}")
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    "User already exists"
                )
            }

            userInformationRepository.registerUser(
                userPhone = receive.phoneNumber,
                userName = receive.userName,
                password = receive.password,
                login = receive.login
            )

            registrationLogger.info("User registered successfully: ${receive.phoneNumber}")

            call.respond(
                HttpStatusCode.Created,
                mapOf("message" to "User registered successfully")
            )
        }
    }
}