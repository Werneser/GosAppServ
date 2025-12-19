package gosuslugi.database

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction


object UserInformation : Table() {
    val userId = varchar("userId", 50).references(UserCredentials.userId)
    val userPhone = varchar("userPhone", 50)
    val userName = varchar("userName", 50)

    override val primaryKey = PrimaryKey(userId)
}



object UserCredentials : Table() {
    val userId = varchar("userId", 50)
    val login = varchar("login", 50)
    val password = varchar("password", 255)
    val token = varchar("token", 255).nullable()

    override val primaryKey = PrimaryKey(userId)
}

object Appointments : Table() {
    val id = varchar("id", 50)
    val user = varchar("user", 50)
    val serviceId = varchar("service_id", 50)
    val serviceTitle = varchar("service_title", 255)
    val serviceDescription = text("service_description")
    val serviceCategory = integer("service_category")
    val appliedAt = varchar("applied_at", 50)
    val status = integer("status")
    val formData = text("form_data").nullable()
}

object Invoices : Table() {
    val id = varchar("id", 50)
    val user = varchar("user", 50)
    val serviceName = varchar("service_name", 255)
    val invoiceNumber = varchar("invoice_number", 50)
    val status = integer("status")
    val amount = double("amount")
    val issueAddress = varchar("issue_address", 255)
    val destinationAddress = varchar("destination_address", 255)
}


fun createTables() {
    transaction {
        SchemaUtils.create(Invoices, UserInformation, UserCredentials, Appointments)
    }
}

fun recreateTables() {
    transaction {
        SchemaUtils.drop(Invoices, UserInformation, UserCredentials, Appointments)
        SchemaUtils.create(Invoices, UserInformation, UserCredentials, Appointments)
    }
}
