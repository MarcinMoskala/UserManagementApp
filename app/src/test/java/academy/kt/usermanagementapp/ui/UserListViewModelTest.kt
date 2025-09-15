package academy.kt.usermanagementapp.ui

import academy.kt.usermanagementapp.MainCoroutineRule
import academy.kt.usermanagementapp.TestData
import academy.kt.usermanagementapp.data.network.ApiException
import academy.kt.usermanagementapp.fakes.FakeDelayedUserRepository
import academy.kt.usermanagementapp.model.AddUser
import academy.kt.usermanagementapp.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var userRepository: FakeDelayedUserRepository
    private lateinit var viewModel: UserListViewModel

    @Before
    fun setup() {
        userRepository = FakeDelayedUserRepository()
        viewModel = UserListViewModel(userRepository)
    }

    @Test
    fun `should load and display users during initialization`() {
        // given
        userRepository.hasUsers(TestData.users1)

        // when process has started
        coroutineRule.scheduler.runCurrent()

        // then
        assertEquals(true, viewModel.showLoading.value)
        assertEquals(null, viewModel.error.value)

        // when advance until coroutine is finished
        coroutineRule.scheduler.advanceUntilIdle()

        // then users are displayed
        assertEquals(TestData.users1, viewModel.usersList.value)
        assertEquals(null, viewModel.error.value)
        assertEquals(false, viewModel.showLoading.value)
        assertEquals(
            FakeDelayedUserRepository.Companion.FETCH_USERS_DELAY,
            coroutineRule.scheduler.currentTime
        )
    }

    @Test
    fun `should show error and empty list when loading failed`() {
        // given
        val anException = ApiException(500, "Some message")
        userRepository.fetchUsersFailure = anException

        // when
        coroutineRule.scheduler.advanceUntilIdle()

        // then
        assertEquals(emptyList(), viewModel.usersList.value)
        assertEquals(false, viewModel.showLoading.value)
        assertEquals(anException, viewModel.error.value)
    }

    @Test
    fun `when asked to refresh, should load new users`() {
        val startTime = givenLoadingUsersHasFinished(TestData.users1)

        // when users has changed and we ask to refresh
        userRepository.hasUsers(TestData.users2)
        viewModel.refresh()

        // when process is started
        coroutineRule.scheduler.runCurrent()

        // then is loading
        assertEquals(true, viewModel.showLoading.value)
        assertEquals(null, viewModel.error.value)

        // when advance until coroutine is finished
        coroutineRule.scheduler.advanceUntilIdle()

        // then users are displayed
        assertEquals(TestData.users2, viewModel.usersList.value)
        assertEquals(false, viewModel.showLoading.value)
        assertEquals(null, viewModel.error.value)
        assertEquals(
            FakeDelayedUserRepository.Companion.FETCH_USERS_DELAY,
            coroutineRule.scheduler.currentTime - startTime
        )
    }

    @Test
    fun `when adding user, should add user to repository and load new users`() {
        val startTime = givenLoadingUsersHasFinished(TestData.users1)
        val aName = "John"
        val anEmail = "joe@hunt.com"

        // when
        viewModel.addUser(AddUser(aName, anEmail))

        // when advance until adding process is finished
        coroutineRule.scheduler.advanceTimeBy(FakeDelayedUserRepository.Companion.ADD_USER_DELAY)
        coroutineRule.scheduler.runCurrent()

        // then new user is in repository
        with(userRepository.users.last()) {
            assertEquals(aName, name)
            assertEquals(anEmail, email)
        }

        // when
        coroutineRule.scheduler.advanceUntilIdle()

        // then users are displayed
        val lastDisplayedUser = viewModel.usersList.value.last()
        assertEquals(aName, lastDisplayedUser.name)
        assertEquals(anEmail, lastDisplayedUser.email)
        assertEquals(null, viewModel.error.value)
        assertEquals(
            FakeDelayedUserRepository.Companion.FETCH_USERS_DELAY + FakeDelayedUserRepository.Companion.ADD_USER_DELAY,
            coroutineRule.scheduler.currentTime - startTime
        )
    }

    @Test
    fun `when adding user, should not display loading`() {
        givenLoadingUsersHasFinished(TestData.users1)

        // when
        viewModel.addUser(AddUser("John", "joe@hunt.com"))

        assertEquals(false, viewModel.showLoading.value)
        coroutineRule.scheduler.runCurrent()
        assertEquals(false, viewModel.showLoading.value)
        coroutineRule.scheduler.advanceTimeBy(FakeDelayedUserRepository.Companion.ADD_USER_DELAY)
        assertEquals(false, viewModel.showLoading.value)
        coroutineRule.scheduler.runCurrent()
        assertEquals(false, viewModel.showLoading.value)
        coroutineRule.scheduler.advanceTimeBy(FakeDelayedUserRepository.Companion.FETCH_USERS_DELAY)
        assertEquals(false, viewModel.showLoading.value)
    }

    @Test
    fun `when adding user, should show errors`() {
        givenLoadingUsersHasFinished(TestData.users1)

        // and adding endpoint is failing
        val anException = ApiException(500, "Some message")
        userRepository.addUserFailure = anException

        // when
        viewModel.addUser(AddUser("John", "joe@hunt.com"))

        // when advance until adding process is finished
        coroutineRule.scheduler.advanceUntilIdle()

        // then
        assertEquals(anException, viewModel.error.value)
    }

    @Test
    fun `when removing user, should add user to repository and load new users`() {
        val startTime = givenLoadingUsersHasFinished(TestData.users1)
        val userToRemove = TestData.users1.first()
        val usersAfterRemoving = TestData.users1 - userToRemove

        // when
        viewModel.removeUser(userToRemove.id)

        // when advance until adding process is finished
        coroutineRule.scheduler.advanceTimeBy(FakeDelayedUserRepository.Companion.REMOVE_USER_DELAY)
        coroutineRule.scheduler.runCurrent()

        // user removed from repository
        assertEquals(usersAfterRemoving, userRepository.users)

        // when
        coroutineRule.scheduler.advanceUntilIdle()

        // then new users are displayed
        assertEquals(usersAfterRemoving, viewModel.usersList.value)
        assertEquals(false, viewModel.showLoading.value)
        assertEquals(null, viewModel.error.value)
        assertEquals(
            FakeDelayedUserRepository.Companion.FETCH_USERS_DELAY + FakeDelayedUserRepository.Companion.REMOVE_USER_DELAY,
            coroutineRule.scheduler.currentTime - startTime
        )
    }

    @Test
    fun `when removing user, should show errors`() {
        givenLoadingUsersHasFinished(TestData.users1)

        // and adding endpoint is failing
        val anException = ApiException(500, "Some message")
        userRepository.removeUserFailure = anException

        // when
        viewModel.removeUser(123)

        // when advance until adding process is finished
        coroutineRule.scheduler.advanceUntilIdle()

        // then
        assertEquals(anException, viewModel.error.value)
    }

    @Test
    fun `when removing user, should not display loading`() {
        givenLoadingUsersHasFinished(TestData.users1)

        // when
        viewModel.removeUser(TestData.users1.first().id)

        assertEquals(false, viewModel.showLoading.value)
        coroutineRule.scheduler.runCurrent()
        assertEquals(false, viewModel.showLoading.value)
        coroutineRule.scheduler.advanceTimeBy(FakeDelayedUserRepository.Companion.REMOVE_USER_DELAY)
        assertEquals(false, viewModel.showLoading.value)
        coroutineRule.scheduler.runCurrent()
        assertEquals(false, viewModel.showLoading.value)
        coroutineRule.scheduler.advanceTimeBy(FakeDelayedUserRepository.Companion.FETCH_USERS_DELAY)
        assertEquals(false, viewModel.showLoading.value)
    }

    private fun givenLoadingUsersHasFinished(users: List<User>): Long {
        userRepository.hasUsers(users)
        coroutineRule.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel.showLoading.value)
        assertEquals(users, viewModel.usersList.value)
        assertEquals(null, viewModel.error.value)
        return coroutineRule.scheduler.currentTime
    }
}