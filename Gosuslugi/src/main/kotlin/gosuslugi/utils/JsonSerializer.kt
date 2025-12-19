package gosuslugi.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import gosuslugi.features.userinformation.UserInformationResponse

object JsonSerializer {
    private val json = Json { prettyPrint = true }

    fun serializeUserInformationResponse(response: UserInformationResponse): String {
        return json.encodeToString(response)
    }
}
