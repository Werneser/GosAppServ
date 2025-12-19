package gosuslugi.features.userinformation

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import gosuslugi.cache.InMemoryCache
import gosuslugi.repository.UserInformationRepository

fun Application.configureUserInformationRouting() {
    val userRepository = UserInformationRepository()

    routing {
        route("/user") {
            get("/info") {
                val token = call.request.headers["Authorization"] ?: ""

                val user = userRepository.getUserInfo(token) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    UserInformationResponse(false, "Пользователь не найден")
                )

                call.respond(
                    UserInformationResponse(
                        success = true,
                        user = UserResponse(
                            userId = token,
                            phone = user.userPhone,
                            name = user.userName,
                        )
                    )
                )
            }

            get("/{login}") {
                val login = call.parameters["login"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    UserInformationResponse(false, "Не указан логин")
                )

                val user = userRepository.getUserByLogin(login) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    UserInformationResponse(false, "Пользователь не найден")
                )

                call.respond(
                    UserInformationResponse(
                        success = true,
                        user = UserResponse(
                            userId = user.userId,
                            phone = user.userPhone,
                            name = user.userName,
                        )
                    )
                )
            }

            put("/{login}") {

                val login = call.parameters["login"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    UserInformationResponse(false, "Не указан email")
                )

                val params = call.receive<UpdateUserRequest>()
                val name = params.name ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    UserInformationResponse(false, "Не указано имя")
                )

                userRepository.updateUser(login, name)
                call.respond(UserInformationResponse(true, "Данные пользователя обновлены"))
            }

            delete("/{phone}") {
                val token = call.request.headers["Authorization"] ?: ""
                if (!validateToken(token)) {
                    return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        UserInformationResponse(false, "Не авторизован")
                    )
                }

                val phone = call.parameters["phone"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    UserInformationResponse(false, "Не указан телефон")
                )

                userRepository.deleteUser(phone)
                call.respond(UserInformationResponse(true, "Аккаунт удален"))
            }
        }
    }
}

data class UpdateUserRequest(
    val name: String,
    val sName: String,
)

private fun validateToken(token: String): Boolean {
    return InMemoryCache.token.any { it.token == token }
}

