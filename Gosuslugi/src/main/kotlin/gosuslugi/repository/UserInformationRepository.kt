package gosuslugi.repository

import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import gosuslugi.database.UserInformation
import gosuslugi.DTOs.UserInformationDTO
import gosuslugi.database.UserCredentials
import gosuslugi.database.UserProfile
import gosuslugi.cache.InMemoryCache
import org.jetbrains.exposed.sql.*
import java.util.*

val logger = LoggerFactory.getLogger("UserRegistrationLogger")

class UserInformationRepository {
    private val bcryptHasher = BCrypt.withDefaults()
    private val bcryptVerifier = BCrypt.verifyer()

    fun getUserByPhone(email: String): UserInformationDTO? = transaction {
        (UserInformation innerJoin UserCredentials)
            .select { UserInformation.userPhone eq email }
            .map { toUserInformationDTO(it) }
            .singleOrNull()
    }

    fun getUserInfo(token: String): UserInformationDTO? = transaction {
        (UserInformation innerJoin UserCredentials)
            .select { UserCredentials.token eq token }
            .map { toUserInformationDTO(it) }
            .singleOrNull()
    }

    fun getUserByLogin(login: String): UserInformationDTO? = transaction {
        (UserInformation innerJoin UserCredentials)
            .select { UserCredentials.login eq login }
            .map { toUserInformationDTO(it) }
            .singleOrNull()
    }

    private fun toUserInformationDTO(row: ResultRow): UserInformationDTO {
        return UserInformationDTO(
            userId = row[UserInformation.userId],
            userPhone = row[UserInformation.userPhone],
            userName = row[UserInformation.userName],
            role = row[UserInformation.role]
        )
    }

    fun registerUser(userPhone: String, userName: String, login: String, password: String, role: Int = 0) {
        val userId = UUID.randomUUID().toString()
        val token = UUID.randomUUID().toString()
        transaction {
            UserCredentials.insert {
                it[UserCredentials.userId] = userId
                it[UserCredentials.login] = login
                it[UserCredentials.password] = hashPassword(password)
                it[UserCredentials.token] = token
            }
            UserInformation.insert {
                it[UserInformation.userId] = userId
                it[UserInformation.userPhone] = userPhone
                it[UserInformation.userName] = userName
                it[UserInformation.role] = role
            }
            UserProfile.insert {
                it[UserProfile.userId] = userId
                it[UserProfile.fullName] = userName
                it[UserProfile.phone] = userPhone
            }
        }
        InMemoryCache.addToken(token, userId, login)
    }

    fun hashPassword(password: String): String {
        return bcryptHasher.hashToString(12, password.toCharArray())
    }

    fun loginUser(login: String, password: String): Pair<String?, Int> {
        val user = transaction {
            (UserCredentials innerJoin UserInformation)
                .select { UserCredentials.login eq login }
                .singleOrNull()
        } ?: return Pair(null, 0)

        val hashedPassword = user[UserCredentials.password]
        if (verifyPassword(password, hashedPassword)) {
            val token = user[UserCredentials.token]
            val userId = user[UserCredentials.userId]
            val role = user[UserInformation.role]
            InMemoryCache.addToken(token.toString(), userId, login)
            return Pair(token, role)
        }
        return Pair(null, 0)
    }

    fun verifyPassword(inputPassword: String, hashedPassword: String): Boolean {
        return bcryptVerifier.verify(inputPassword.toCharArray(), hashedPassword).verified
    }

    fun deleteUser(userId: String) {
        transaction {
            UserCredentials.deleteWhere { UserCredentials.userId eq userId }
            UserInformation.deleteWhere { UserInformation.userId eq userId }
        }
    }

    fun deleteUserByLogin(login: String): Boolean {
        return transaction {
            val userCredential = UserCredentials.select { UserCredentials.login eq login }.singleOrNull()
            if (userCredential != null) {
                val userId = userCredential[UserCredentials.userId]
                UserInformation.deleteWhere { UserInformation.userId eq userId }
                UserCredentials.deleteWhere { UserCredentials.userId eq userId }
                true
            } else {
                false
            }
        }
    }

    fun updateUser(userEmail: String, newName: String) {
        transaction {
            UserInformation.update({ UserInformation.userPhone eq userEmail }) {
                it[UserInformation.userName] = newName
            }
        }
    }

    fun getAllUsers(): List<Map<String, String>> = transaction {
        UserInformation.selectAll().map {
            mapOf(
                "login" to it[UserInformation.userName], // Временно используем userName как login
                "name" to it[UserInformation.userName],
                "phone" to it[UserInformation.userPhone],
                "userId" to it[UserInformation.userId]
            )
        }
    }

    fun getAllUsersWithLogin(): List<Map<String, String>> = transaction {
        (UserInformation innerJoin UserCredentials)
            .selectAll()
            .map {
                mapOf(
                    "login" to it[UserCredentials.login],
                    "name" to it[UserInformation.userName],
                    "phone" to it[UserInformation.userPhone],
                    "userId" to it[UserInformation.userId]
                )
            }
    }
}