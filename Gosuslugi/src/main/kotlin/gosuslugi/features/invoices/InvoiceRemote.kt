package gosuslugi.features.invoices

import gosuslugi.DTOs.InvoiceDTO
import kotlinx.serialization.Serializable

@Serializable
data class InvoiceReceiveRemote(
    val user: String,
    val serviceName: String,
    val invoiceNumber: String,
    val status: Int,
    val amount: Double,
    val issueAddress: String,
    val destinationAddress: String
)

@Serializable
data class InvoiceResponseRemote(
    val message: String,
    val invoice: InvoiceDTO? = null
)
