package gosuslugi.features.registration

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import gosuslugi.database.recreateTables
import gosuslugi.module
import gosuslugi.database.initDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.*

@Serializable
data class LoginResponse(val token: String)

@Serializable
data class UserInfoResponse(
    val success: Boolean,
    val message: String? = null,
    val user: UserData? = null
)

@Serializable
data class UserData(
    val userId: String,
    val phone: String,
    val name: String
)

class RegistrationUnitTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    @BeforeTest
    fun setup() {
        initDatabase()
        recreateTables()
    }

    @Test
    fun testRegistrationSuccess() = testApplication {
        application {
            module()
        }

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "login": "testuser",
                    "phoneNumber": "+79001234567",
                    "password": "securePassword123",
                    "userName": "Иван Иванов"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("User registered successfully"))
    }

    @Test
    fun testRegistrationDuplicateUser() = testApplication {
        application {
            module()
        }

        val registerRequest = """
            {
                "login": "duplicateuser",
                "phoneNumber": "+79001234567",
                "password": "password123",
                "userName": "Петр Петров"
            }
        """.trimIndent()

        val firstResponse = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(registerRequest)
        }
        assertEquals(HttpStatusCode.Created, firstResponse.status)

        val secondResponse = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(registerRequest)
        }

        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
        assertTrue(secondResponse.bodyAsText().contains("User already exists"))
    }

    @Test
    fun testRegistrationInvalidPhoneFormat() = testApplication {
        application {
            module()
        }

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "login": "badphoneuser",
                    "phoneNumber": "12345",
                    "password": "password123",
                    "userName": "Сергей Сидоров"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Phone is not valid"))
    }


    @Test
    fun testRegistrationMissingRequiredFields() = testApplication {
        application {
            module()
        }

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "login": "missingfields",
                    "phoneNumber": "+79001234567"
                }
            """.trimIndent())
        }

        assertTrue(
            response.status == HttpStatusCode.BadRequest ||
                    response.status == HttpStatusCode.InternalServerError
        )
    }
    @Test
    fun testRegistrationWithNullValues() = testApplication {
        application {
            module()
        }

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "login": null,
                    "phoneNumber": null,
                    "password": null,
                    "userName": null
                }
            """.trimIndent())
        }

        assertTrue(
            response.status == HttpStatusCode.BadRequest ||
                    response.status == HttpStatusCode.InternalServerError
        )
    }
}