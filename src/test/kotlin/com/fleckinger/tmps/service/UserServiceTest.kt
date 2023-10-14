package com.fleckinger.tmps.service

import com.fleckinger.tmps.exception.RegistrationNotCompletedException
import com.fleckinger.tmps.model.User
import com.fleckinger.tmps.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import java.time.ZoneId
import java.util.*

class UserServiceTest {

    @Mock
    lateinit var userRepositoryMock: UserRepository

    @InjectMocks
    lateinit var userService: UserService

    init {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun getUser_shouldReturnUser() {
        val id = UUID.randomUUID()
        val user = User()
        user.id = id

        `when`(userRepositoryMock.findById(id)).thenReturn(Optional.of(user))

        assertEquals(user, userService.getUser(id.toString()))
    }

    @Test
    fun getUser_shouldThrowEntityNotFoundException_ifUserNotFound() {
        val id = UUID.randomUUID()

        `when`(userRepositoryMock.findById(id)).thenReturn(Optional.empty())

        assertThrows<EntityNotFoundException> { userService.getUser(id.toString()) }
    }

    @Test
    fun getUserByTelegramUserId_shouldReturnUser() {
        val telegramUserId = 100L
        val user = User()
        user.telegramUserId = telegramUserId

        `when`(userRepositoryMock.findUserByTelegramUserId(telegramUserId)).thenReturn(Optional.of(user))

        assertEquals(user, userService.getUserByTelegramUserId(telegramUserId))
    }

    @Test
    fun getUserByTelegramUserId_shouldThrowEntityNotFoundException_ifUserNotFound() {
        val telegramUserId = 100L

        `when`(userRepositoryMock.findUserByTelegramUserId(telegramUserId)).thenReturn(Optional.empty())

        assertThrows<EntityNotFoundException> { userService.getUserByTelegramUserId(telegramUserId) }
    }

    @Test
    fun exists_shouldReturnTrue_ifUserExistsByTelegramUserId() {
        val telegramUserId = 100L

        `when`(userRepositoryMock.existsByTelegramUserId(telegramUserId)).thenReturn(true)

        assertEquals(true, userService.exists(telegramUserId))
    }

    @Test
    fun exists_shouldReturnFalse_ifUserNotExistsByTelegramUserId() {
        val telegramUserId = 100L

        `when`(userRepositoryMock.existsByTelegramUserId(telegramUserId)).thenReturn(false)

        assertEquals(false, userService.exists(telegramUserId))
    }

    @Test
    fun createUser_shouldCreateNewUserAndReturnIt() {
        val telegramUserId = 100L
        val username = "username"
        val user = User(telegramUserId = telegramUserId, username = username)

        `when`(userRepositoryMock.save(any())).thenAnswer{ a -> a.arguments[0]}

        assertEquals(user, userService.createNewUser(telegramUserId, username))
    }

    @Test
    fun saveUser_shouldSaveUserAndReturnIt() {
        val telegramUserId = 100L
        val username = "username"
        val user = User(telegramUserId = telegramUserId, username = username)

        `when`(userRepositoryMock.save(any())).thenAnswer{ a -> a.arguments[0]}

        assertEquals(user, userService.saveUser(user))
        verify(userRepositoryMock).save(user)
    }

    @Test
    fun setChannelId_shouldSetNewChannelId_saveUser_andReturnChannelId() {
        val telegramUserId = 100L
        val newChannelId = 200L
        val user = User()
        user.telegramUserId = telegramUserId
        val editedUser = User()
        editedUser.telegramUserId = telegramUserId
        editedUser.channelId = newChannelId

        `when`(userRepositoryMock.findUserByTelegramUserId(telegramUserId)).thenReturn(Optional.of(user))
        `when`(userRepositoryMock.save(any())).thenAnswer{ a -> a.arguments[0] }

        assertEquals(newChannelId, userService.setChannelId(newChannelId, telegramUserId))
        verify(userRepositoryMock).save(editedUser)
    }

    @Test
    fun setTimezone_shouldSetNewTimezone_saveUser_andReturnTimezone() {
        val telegramUserId = 100L
        val zoneId = ZoneId.of("Asia/Aden")
        val user = User()
        user.telegramUserId = telegramUserId
        val editedUser = User()
        editedUser.telegramUserId = telegramUserId
        editedUser.timeZone = zoneId.id

        `when`(userRepositoryMock.findUserByTelegramUserId(telegramUserId)).thenReturn(Optional.of(user))
        `when`(userRepositoryMock.save(any())).thenAnswer{ a -> a.arguments[0] }

        assertEquals(zoneId.id, userService.setTimezone(telegramUserId, zoneId))
        verify(userRepositoryMock).save(editedUser)
    }

    @Test
    fun checkUserRegistration_shouldThrowRegistrationNotCompletedException_ifUserChannelIdIsNull() {
        val user = User(timeZone = "")

        assertThrows<RegistrationNotCompletedException> { userService.checkUserRegistration(user) }
    }

    @Test
    fun checkUserRegistration_shouldThrowRegistrationNotCompletedException_ifUserTimezoneIsNull() {
        val user = User(channelId = 100L)

        assertThrows<RegistrationNotCompletedException> { userService.checkUserRegistration(user) }
    }

    @Test
    fun checkUserRegistration_shouldntThrowRegistrationNotCompletedException_ifUserTimezoneAndChannelIdNotNull() {
        val user = User(channelId = 100L, timeZone = "")

        assertDoesNotThrow {userService.checkUserRegistration(user)}
    }
}