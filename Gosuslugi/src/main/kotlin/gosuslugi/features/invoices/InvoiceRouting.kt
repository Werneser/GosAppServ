package gosuslugi.features.invoices

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import gosuslugi.repository.InvoiceInformationRepository
import gosuslugi.features.invoices.InvoiceReceiveRemote
import gosuslugi.features.invoices.InvoiceResponseRemote
import gosuslugi.repository.InvoiceStatus

val invoiceLogger = LoggerFactory.getLogger("InvoiceRouteLogger")

fun Application.configureInvoiceRouting() {
    val invoiceRepository = InvoiceInformationRepository()

    routing {
        // Получение всех квитанций пользователя
        get("/invoices/user/{username}") {
            val username = call.parameters["username"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Missing username"
            )
            val invoices = invoiceRepository.getInvoicesByUser(username)
            call.respond(HttpStatusCode.OK, invoices)
        }

        // Создание новой квитанции
        post("/invoices") {
            val receive = try {
                call.receive<InvoiceReceiveRemote>()
            } catch (e: Exception) {
                invoiceLogger.error("Invoice creation error: ${e.message}")
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid request format")
            }

            val invoice = invoiceRepository.createInvoice(
                user = receive.user,
                serviceName = receive.serviceName,
                invoiceNumber = receive.invoiceNumber,
                status = receive.status,
                amount = receive.amount,
                issueAddress = receive.issueAddress,
                destinationAddress = receive.destinationAddress
            )

            invoiceLogger.info("Invoice created for user: ${receive.user}")
            call.respond(
                HttpStatusCode.Created,
                InvoiceResponseRemote(
                    message = "Invoice created successfully",
                    invoice = invoice
                )
            )
        }

        // Обновление статуса квитанции
        patch("/invoices/{id}/status") {
            val id = call.parameters["id"] ?: return@patch call.respond(
                HttpStatusCode.BadRequest,
                "Missing invoice ID"
            )
            val status = try {
                val statusIndex = call.request.queryParameters["status"]?.toIntOrNull()
                InvoiceStatus.values().find { it.ordinal == statusIndex }
            } catch (e: Exception) {
                return@patch call.respond(HttpStatusCode.BadRequest, "Invalid status")
            } ?: return@patch call.respond(HttpStatusCode.BadRequest, "Missing status")

            invoiceRepository.updateInvoiceStatus(id, status)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Status updated successfully"))
        }
    }
}
