package gosuslugi.repository

import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import gosuslugi.database.UserInformation
import gosuslugi.DTOs.UserInformationDTO
import gosuslugi.database.UserCredentials
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
        )
    }

    fun registerUser(userPhone: String, userName: String, login: String, password: String) {
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
            }
        }
    }

    fun hashPassword(password: String): String {
        return bcryptHasher.hashToString(12, password.toCharArray())
    }

    fun loginUser(login: String, password: String): String? {
        val user = transaction {
            UserCredentials.select { UserCredentials.login eq login }
                .singleOrNull()
        } ?: return null

        val hashedPassword = user[UserCredentials.password]
        if (verifyPassword(password, hashedPassword)) {
            return user[UserCredentials.token]
        }
        return null
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

    fun updateUser(userEmail: String, newName: String) {
        transaction {
            UserInformation.update({ UserInformation.userPhone eq userEmail }) {
                it[UserInformation.userName] = newName
            }
        }
    }
}
