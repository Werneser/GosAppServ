package gosuslugi

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.serialization.gson.*
import io.ktor.server.plugins.contentnegotiation.*
import gosuslugi.features.login.configureLoginRouting
import gosuslugi.features.registration.configureRegistrationRouting
import org.h2.tools.Console
import org.jetbrains.exposed.sql.transactions.transaction
import gosuslugi.database.initDatabase
import gosuslugi.features.appointments.configureAppointmentRouting
import gosuslugi.features.invoices.configureInvoiceRouting
import gosuslugi.features.userinformation.configureUserInformationRouting
import gosuslugi.features.userprofile.configureUserProfileRouting

fun main() {
    initDatabase()

    Console.main("-web", "-webAllowOthers")

    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    initDatabase()
    transaction {
        exec("SELECT 1") {}
    }

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    configureRegistrationRouting()
    configureLoginRouting()
    configureUserInformationRouting()
    configureUserProfileRouting()
    configureAppointmentRouting()
    configureInvoiceRouting()
}
