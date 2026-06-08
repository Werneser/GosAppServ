package gosuslugi.features.userprofile

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import gosuslugi.cache.InMemoryCache
import gosuslugi.repository.UserProfileRepository
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("UserProfileRouting")

fun Application.configureUserProfileRouting() {
    val userProfileRepository = UserProfileRepository()

    routing {
        route("/profile") {
            // Получение профиля по токену
            get {
                val token = call.request.headers["Authorization"]
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        UserProfileResponseRemote(false, "Токен не предоставлен")
                    )

                logger.info("Getting profile for token: $token")

                val profile = userProfileRepository.getProfileByToken(token)

                if (profile != null) {
                    logger.info("Profile found: $profile")
                    call.respond(
                        UserProfileResponseRemote(
                            success = true,
                            profile = profile
                        )
                    )
                } else {
                    logger.warn("Profile not found for token: $token")
                    call.respond(
                        UserProfileResponseRemote(
                            success = false,
                            message = "Профиль не найден"
                        )
                    )
                }
            }

            // Обновление профиля
            put {
                val token = call.request.headers["Authorization"]
                    ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        UserProfileResponseRemote(false, "Токен не предоставлен")
                    )

                logger.info("Updating profile for token: $token")

                // Получаем userId по токену
                val userId = InMemoryCache.getUserIdByToken(token)
                    ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        UserProfileResponseRemote(false, "Недействительный токен")
                    )

                val receive = try {
                    call.receive<UserProfileReceiveRemote>()
                } catch (e: Exception) {
                    logger.error("Invalid request format: ${e.message}")
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        UserProfileResponseRemote(false, "Неверный формат данных")
                    )
                }

                logger.info("Updating profile for userId: $userId with data: $receive")

                // Валидация email если он предоставлен
                if (receive.email != null && !isValidEmail(receive.email)) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        UserProfileResponseRemote(false, "Неверный формат email")
                    )
                }

                try {
                    val updatedProfile = userProfileRepository.saveOrUpdateProfile(
                        userId = userId,
                        profile = receive
                    )

                    logger.info("Profile updated successfully: $updatedProfile")

                    call.respond(
                        HttpStatusCode.OK,
                        UserProfileResponseRemote(
                            success = true,
                            message = "Профиль успешно обновлен",
                            profile = updatedProfile
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Error updating profile: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        UserProfileResponseRemote(false, "Ошибка при сохранении профиля: ${e.message}")
                    )
                }
            }

            // Получение профиля по userId (админский метод)
            get("/{userId}") {
                val userId = call.parameters["userId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        UserProfileResponseRemote(false, "Не указан userId")
                    )

                val profile = userProfileRepository.getProfileByUserId(userId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        UserProfileResponseRemote(false, "Профиль не найден")
                    )

                call.respond(
                    UserProfileResponseRemote(
                        success = true,
                        profile = profile
                    )
                )
            }
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return emailRegex.matches(email)
}