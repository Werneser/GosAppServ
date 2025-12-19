package gosuslugi.DTOs

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceDTO(
    val id: String,
    val user: String,
    val serviceName: String,
    val invoiceNumber: String,
    val status: Int,
    val amount: Double,
    val issueAddress: String,
    val destinationAddress: String
)
