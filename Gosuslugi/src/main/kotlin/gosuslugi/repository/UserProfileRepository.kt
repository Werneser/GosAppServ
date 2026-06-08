package gosuslugi.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import gosuslugi.database.UserProfile
import gosuslugi.database.UserCredentials
import gosuslugi.database.UserInformation
import gosuslugi.DTOs.UserProfileDTO

class UserProfileRepository {
    private val logger = LoggerFactory.getLogger(UserProfileRepository::class.java)

    fun getProfileByToken(token: String): UserProfileDTO? = transaction {
        val userCredential = UserCredentials.select { UserCredentials.token eq token }
            .singleOrNull() ?: return@transaction null

        val userId = userCredential[UserCredentials.userId]

        val profile = UserProfile.select { UserProfile.userId eq userId }
            .singleOrNull()

        if (profile != null) {
            toUserProfileDTO(profile, userId)
        } else {
            // Если профиль не найден, пробуем получить данные из UserInformation
            val userInfo = UserInformation.select { UserInformation.userId eq userId }
                .singleOrNull()

            if (userInfo != null) {
                // Создаем профиль на основе данных регистрации
                UserProfile.insert {
                    it[UserProfile.userId] = userId
                    it[UserProfile.fullName] = userInfo[UserInformation.userName]
                    it[UserProfile.phone] = userInfo[UserInformation.userPhone]
                }
                logger.info("Profile auto-created from registration data for user: $userId")

                val newProfile = UserProfile.select { UserProfile.userId eq userId }.single()!!
                toUserProfileDTO(newProfile, userId)
            } else {
                // Создаем пустой профиль
                UserProfile.insert {
                    it[UserProfile.userId] = userId
                }
                logger.info("Empty profile created for user: $userId")

                UserProfileDTO(
                    userId = userId,
                    fullName = null,
                    passport = null,
                    snils = null,
                    phone = null,
                    email = null
                )
            }
        }
    }

    fun getProfileByUserId(userId: String): UserProfileDTO? = transaction {
        val profile = UserProfile.select { UserProfile.userId eq userId }
            .singleOrNull()

        profile?.let { toUserProfileDTO(it, userId) }
    }

    fun saveOrUpdateProfile(userId: String, profile: gosuslugi.features.userprofile.UserProfileReceiveRemote): UserProfileDTO = transaction {
        val existingProfile = UserProfile.select { UserProfile.userId eq userId }
            .singleOrNull()

        if (existingProfile != null) {
            // Обновляем существующий профиль
            UserProfile.update({ UserProfile.userId eq userId }) {
                it[fullName] = profile.fullName
                it[passport] = profile.passport
                it[snils] = profile.snils
                it[phone] = profile.phone
                it[email] = profile.email
            }
            logger.info("Profile updated for user: $userId")
        } else {
            // Создаем новый профиль
            UserProfile.insert {
                it[UserProfile.userId] = userId
                it[fullName] = profile.fullName
                it[passport] = profile.passport
                it[snils] = profile.snils
                it[phone] = profile.phone
                it[email] = profile.email
            }
            logger.info("Profile created for user: $userId")
        }

        // Возвращаем обновленный профиль
        val updatedProfile = UserProfile.select { UserProfile.userId eq userId }
            .single()!!
        toUserProfileDTO(updatedProfile, userId)
    }

    private fun toUserProfileDTO(row: ResultRow, userId: String): UserProfileDTO {
        return UserProfileDTO(
            userId = userId,
            fullName = row[UserProfile.fullName],
            passport = row[UserProfile.passport],
            snils = row[UserProfile.snils],
            phone = row[UserProfile.phone],
            email = row[UserProfile.email]
        )
    }
}