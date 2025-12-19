package gosuslugi.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import gosuslugi.database.Invoices
import gosuslugi.DTOs.InvoiceDTO
import java.util.*

class InvoiceInformationRepository {
    private val logger = LoggerFactory.getLogger(InvoiceInformationRepository::class.java)

    fun getInvoicesByUser(user: String): List<InvoiceDTO> = transaction {
        Invoices.select { Invoices.user eq user }
            .map { toInvoiceDTO(it) }
    }

    fun createInvoice(
        user: String,
        serviceName: String,
        invoiceNumber: String,
        status: Int,
        amount: Double,
        issueAddress: String,
        destinationAddress: String
    ): InvoiceDTO {
        val invoiceId = UUID.randomUUID().toString()

        transaction {
            Invoices.insert {
                it[id] = invoiceId
                it[Invoices.user] = user
                it[Invoices.serviceName] = serviceName
                it[Invoices.invoiceNumber] = invoiceNumber
                it[Invoices.status] = status
                it[Invoices.amount] = amount
                it[Invoices.issueAddress] = issueAddress
                it[Invoices.destinationAddress] = destinationAddress
            }
            logger.info("Invoice created for user: $user with service: $serviceName")
        }

        return getInvoiceById(invoiceId)!!
    }

    fun getInvoiceById(id: String): InvoiceDTO? = transaction {
        Invoices.select { Invoices.id eq id }
            .map { toInvoiceDTO(it) }
            .singleOrNull()
    }

    fun updateInvoiceStatus(id: String, status: InvoiceStatus) = transaction {
        Invoices.update({ Invoices.id eq id }) {
            it[Invoices.status] = status.ordinal
        }
    }

    private fun toInvoiceDTO(row: ResultRow): InvoiceDTO {
        return InvoiceDTO(
            id = row[Invoices.id],
            user = row[Invoices.user],
            serviceName = row[Invoices.serviceName],
            invoiceNumber = row[Invoices.invoiceNumber],
            status = row[Invoices.status],
            amount = row[Invoices.amount],
            issueAddress = row[Invoices.issueAddress],
            destinationAddress = row[Invoices.destinationAddress]
        )
    }
}

enum class InvoiceStatus {
    UNPAID,
    PAID,
    OVERDUE
}
