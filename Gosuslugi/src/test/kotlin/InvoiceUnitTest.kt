package gosuslugi.features.invoices

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
data class InvoiceResponse(
    val message: String,
    val invoice: InvoiceData? = null
)

@Serializable
data class InvoiceData(
    val id: String,
    val user: String,
    val serviceName: String,
    val invoiceNumber: String,
    val status: Int,
    val amount: Double,
    val issueAddress: String,
    val destinationAddress: String
)

@Serializable
data class StatusUpdateResponse(val message: String)

class InvoiceUnitTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    @BeforeTest
    fun setup() {
        initDatabase()
        recreateTables()
    }

    private suspend fun registerAndGetToken(
        client: io.ktor.client.HttpClient,
        login: String,
        phoneNumber: String,
        password: String,
        userName: String
    ): String {
        val registerResponse = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "login": "$login",
                    "phoneNumber": "$phoneNumber",
                    "password": "$password",
                    "userName": "$userName"
                }
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val loginResponse = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "login": "$login",
                    "password": "$password"
                }
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        val loginData = json.decodeFromString<LoginResponse>(loginResponse.bodyAsText())
        return loginData.token
    }

    @Test
    fun testCreateInvoiceSuccess() = testApplication {
        application {
            module()
        }

        val login = "invoiceuser_${System.currentTimeMillis()}"
        val phoneNumber = "+79001234567"
        val password = "securePassword123"
        val userName = "Иван Иванов"

        registerAndGetToken(client, login, phoneNumber, password, userName)

        val response = client.post("/invoices") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "user": "$login",
                    "serviceName": "Оплата налога на имущество",
                    "invoiceNumber": "INV-2024-001",
                    "status": 0,
                    "amount": 1500.50,
                    "issueAddress": "г. Москва, ул. Тверская, д. 13",
                    "destinationAddress": "г. Москва, ул. Ленина, д. 25, кв. 42"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val responseBody = response.bodyAsText()
        val invoiceResponse = json.decodeFromString<InvoiceResponse>(responseBody)

        assertEquals("Invoice created successfully", invoiceResponse.message)
        assertNotNull(invoiceResponse.invoice)
        assertEquals(login, invoiceResponse.invoice?.user)
        assertEquals("Оплата налога на имущество", invoiceResponse.invoice?.serviceName)
        assertEquals("INV-2024-001", invoiceResponse.invoice?.invoiceNumber)
        assertEquals(0, invoiceResponse.invoice?.status)
        assertEquals(1500.50, invoiceResponse.invoice?.amount)
    }

    @Test
    fun testCreateInvoiceInvalidJson() = testApplication {
        application {
            module()
        }

        val login = "invalidjson_${System.currentTimeMillis()}"
        val phoneNumber = "+79001234568"
        val password = "securePassword123"
        val userName = "Петр Петров"

        registerAndGetToken(client, login, phoneNumber, password, userName)

        val response = client.post("/invoices") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "user": "$login",
                    "serviceName": "Штраф ГИБДД",
                    "invoiceNumber": "INV-2024-002",
                    "status": 0,
                    "amount": 500.00,
                    "issueAddress": "г. Москва, ул. Садовая, д. 10"
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Invalid request format"))
    }

    @Test
    fun testGetUserInvoices() = testApplication {
        application {
            module()
        }

        val login = "getinvoices_${System.currentTimeMillis()}"
        val phoneNumber = "+79001234569"
        val password = "securePassword123"
        val userName = "Сергей Сидоров"

        registerAndGetToken(client, login, phoneNumber, password, userName)

        val invoice1 = client.post("/invoices") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "user": "$login",
                    "serviceName": "Налог на транспорт",
                    "invoiceNumber": "INV-2024-003",
                    "status": 0,
                    "amount": 3500.00,
                    "issueAddress": "г. Москва, ул. Пушкина, д. 5",
                    "destinationAddress": "г. Москва, ул. Мира, д. 12, кв. 8"
                }
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.Created, invoice1.status)

        val invoice2 = client.post("/invoices") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "user": "$login",
                    "serviceName": "Госпошлина за загранпаспорт",
                    "invoiceNumber": "INV-2024-004",
                    "status": 0,
                    "amount": 5000.00,
                    "issueAddress": "г. Москва, ул. Новый Арбат, д. 36",
                    "destinationAddress": "г. Москва, ул. Тверская, д. 15, кв. 77"
                }
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.Created, invoice2.status)

        val getResponse = client.get("/invoices/user/$login")

        assertEquals(HttpStatusCode.OK, getResponse.status)

        val responseBody = getResponse.bodyAsText()
        assertTrue(responseBody.contains("Налог на транспорт"))
        assertTrue(responseBody.contains("Госпошлина за загранпаспорт"))
        assertTrue(responseBody.contains("INV-2024-003"))
        assertTrue(responseBody.contains("INV-2024-004"))
    }

    @Test
    fun testUpdateInvoiceStatus() = testApplication {
        application {
            module()
        }

        val login = "updatestatus_${System.currentTimeMillis()}"
        val phoneNumber = "+79001234570"
        val password = "securePassword123"
        val userName = "Анна Смирнова"

        registerAndGetToken(client, login, phoneNumber, password, userName)

        val createResponse = client.post("/invoices") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "user": "$login",
                    "serviceName": "Штраф за парковку",
                    "invoiceNumber": "INV-2024-005",
                    "status": 0,
                    "amount": 2500.00,
                    "issueAddress": "г. Москва, ул. Большая Дмитровка, д. 15",
                    "destinationAddress": "г. Москва, ул. Петровка, д. 20, кв. 33"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val invoiceResponse = json.decodeFromString<InvoiceResponse>(createResponse.bodyAsText())
        val invoiceId = invoiceResponse.invoice?.id

        assertNotNull(invoiceId)

        val statusBefore = client.get("/invoices/user/$login")
        assertEquals(HttpStatusCode.OK, statusBefore.status)

        val invoicesBefore = json.decodeFromString<List<InvoiceData>>(statusBefore.bodyAsText())
        val targetInvoiceBefore = invoicesBefore.find { it.id == invoiceId }
        assertNotNull(targetInvoiceBefore)
        assertEquals(0, targetInvoiceBefore.status)

        val updateResponse = client.patch("/invoices/$invoiceId/status?status=1") {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("Status updated successfully"))

        val statusAfter = client.get("/invoices/user/$login")
        assertEquals(HttpStatusCode.OK, statusAfter.status)

        val invoicesAfter = json.decodeFromString<List<InvoiceData>>(statusAfter.bodyAsText())
        val targetInvoiceAfter = invoicesAfter.find { it.id == invoiceId }
        assertNotNull(targetInvoiceAfter)
        assertEquals(1, targetInvoiceAfter.status)
    }


    @Test
    fun testCreateMultipleInvoicesForSameUser() = testApplication {
        application {
            module()
        }

        val login = "multipleinvoices_${System.currentTimeMillis()}"
        val phoneNumber = "+79001234572"
        val password = "securePassword123"
        val userName = "Елена Еленова"

        registerAndGetToken(client, login, phoneNumber, password, userName)

        val invoiceData = listOf(
            mapOf(
                "serviceName" to "Налог на землю",
                "invoiceNumber" to "INV-2024-006",
                "amount" to 1200.00
            ),
            mapOf(
                "serviceName" to "Налог на имущество",
                "invoiceNumber" to "INV-2024-007",
                "amount" to 2300.50
            ),
            mapOf(
                "serviceName" to "Госпошлина за регистрацию",
                "invoiceNumber" to "INV-2024-008",
                "amount" to 2000.00
            )
        )

        invoiceData.forEachIndexed { index, data ->
            val response = client.post("/invoices") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "user": "$login",
                        "serviceName": "${data["serviceName"]}",
                        "invoiceNumber": "${data["invoiceNumber"]}",
                        "status": 0,
                        "amount": ${data["amount"]},
                        "issueAddress": "г. Москва, ул. Строителей, д. ${index + 1}",
                        "destinationAddress": "г. Москва, ул. Нагорная, д. ${index + 10}, кв. ${index + 1}"
                    }
                """.trimIndent())
            }
            assertEquals(HttpStatusCode.Created, response.status)
        }

        val getResponse = client.get("/invoices/user/$login")
        assertEquals(HttpStatusCode.OK, getResponse.status)

        val responseBody = getResponse.bodyAsText()
        assertTrue(responseBody.contains("Налог на землю"))
        assertTrue(responseBody.contains("Налог на имущество"))
        assertTrue(responseBody.contains("Госпошлина за регистрацию"))
        assertTrue(responseBody.contains("INV-2024-006"))
        assertTrue(responseBody.contains("INV-2024-007"))
        assertTrue(responseBody.contains("INV-2024-008"))
    }

    @Test
    fun testGetInvoicesForDifferentUsersIsolation() = testApplication {
        application {
            module()
        }

        val login1 = "user1_${System.currentTimeMillis()}"
        val phone1 = "+79001234575"
        val password1 = "password123"
        val name1 = "Пользователь Один"

        val login2 = "user2_${System.currentTimeMillis()}"
        val phone2 = "+79001234576"
        val password2 = "password456"
        val name2 = "Пользователь Два"

        registerAndGetToken(client, login1, phone1, password1, name1)
        registerAndGetToken(client, login2, phone2, password2, name2)

        client.post("/invoices") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "user": "$login1",
                    "serviceName": "Квитанция пользователя 1",
                    "invoiceNumber": "INV-USER1-001",
                    "status": 0,
                    "amount": 1111.11,
                    "issueAddress": "Адрес 1",
                    "destinationAddress": "Адрес доставки 1"
                }
            """.trimIndent())
        }

        client.post("/invoices") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "user": "$login2",
                    "serviceName": "Квитанция пользователя 2",
                    "invoiceNumber": "INV-USER2-001",
                    "status": 0,
                    "amount": 2222.22,
                    "issueAddress": "Адрес 2",
                    "destinationAddress": "Адрес доставки 2"
                }
            """.trimIndent())
        }

        val user1Invoices = client.get("/invoices/user/$login1")
        assertEquals(HttpStatusCode.OK, user1Invoices.status)
        val user1Body = user1Invoices.bodyAsText()
        assertTrue(user1Body.contains("Квитанция пользователя 1"))
        assertTrue(user1Body.contains("INV-USER1-001"))
        assertFalse(user1Body.contains("Квитанция пользователя 2"))
        assertFalse(user1Body.contains("INV-USER2-001"))

        val user2Invoices = client.get("/invoices/user/$login2")
        assertEquals(HttpStatusCode.OK, user2Invoices.status)
        val user2Body = user2Invoices.bodyAsText()
        assertTrue(user2Body.contains("Квитанция пользователя 2"))
        assertTrue(user2Body.contains("INV-USER2-001"))
        assertFalse(user2Body.contains("Квитанция пользователя 1"))
        assertFalse(user2Body.contains("INV-USER1-001"))
    }
}