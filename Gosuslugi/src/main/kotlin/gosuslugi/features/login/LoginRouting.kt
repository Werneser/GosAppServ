package gosuslugi.features.login

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import gosuslugi.repository.UserInformationRepository
import gosuslugi.features.userinformation.LoginReceiveRemote
import gosuslugi.features.userinformation.LoginResponseRemote

val loginLogger = LoggerFactory.getLogger("LoginRouteLogger")

fun Application.configureLoginRouting() {
    val userInformationRepository = UserInformationRepository()

    routing {
        post("/login") {
            val receive = try {
                call.receive<LoginReceiveRemote>()
            } catch (e: Exception) {
                loginLogger.error("Login error: ${e.message}")
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid request format")
            }

            val (token, role) = userInformationRepository.loginUser(receive.login, receive.password)
            if (token != null) {
                loginLogger.info("User logged in successfully: ${receive.login} with role: $role")
                call.respond(LoginResponseRemote(token = token, role = role))
            } else {
                loginLogger.warn("Login failed for user: ${receive.login}")
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }
    }
}