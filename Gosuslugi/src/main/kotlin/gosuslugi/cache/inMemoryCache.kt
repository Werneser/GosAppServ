package gosuslugi.cache

data class TokenInfo(
    val token: String,
    val userId: String,
    val login: String
)

object InMemoryCache {
    private val tokens = mutableListOf<TokenInfo>()

    fun addToken(token: String, userId: String, login: String) {
        tokens.removeAll { it.login == login }
        tokens.add(TokenInfo(token, userId, login))
    }

    fun removeToken(token: String) {
        tokens.removeAll { it.token == token }
    }

    fun getUserIdByToken(token: String): String? {
        return tokens.find { it.token == token }?.userId
    }

    fun getAllTokens(): List<TokenInfo> = tokens.toList()
}