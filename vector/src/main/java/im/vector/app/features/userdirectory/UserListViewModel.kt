/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.userdirectory

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.isEmail
import im.vector.app.core.extensions.toggle
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.IdentityServiceListener
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem

data class ThreePidUser(
        val email: String,
        val user: User?
)

class UserListViewModel @AssistedInject constructor(@Assisted initialState: UserListViewState,
                                                    private val session: Session) :
    VectorViewModel<UserListViewState, UserListAction, UserListViewEvents>(initialState) {

    private val knownUsersSearch = MutableStateFlow("")
    private val directoryUsersSearch = MutableStateFlow("")
    private val identityServerUsersSearch = MutableStateFlow("")

    @AssistedFactory
    interface Factory {
        fun create(initialState: UserListViewState): UserListViewModel
    }

    companion object : MavericksViewModelFactory<UserListViewModel, UserListViewState> {

        override fun create(viewModelContext: ViewModelContext, state: UserListViewState): UserListViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    private val identityServerListener = object : IdentityServiceListener {
        override fun onIdentityServerChange() {
            withState {
                identityServerUsersSearch.tryEmit(it.searchTerm)
                val identityServerURL = cleanISURL(session.identityService().getCurrentIdentityServerUrl())
                setState {
                    copy(configuredIdentityServer = identityServerURL)
                }
            }
        }
    }

    init {
        observeUsers()
        setState {
            copy(
                    configuredIdentityServer = cleanISURL(session.identityService().getCurrentIdentityServerUrl())
            )
        }
        session.identityService().addListener(identityServerListener)
    }

    private fun cleanISURL(url: String?): String? {
        return url?.removePrefix("https://")
    }

    override fun onCleared() {
        session.identityService().removeListener(identityServerListener)
        super.onCleared()
    }

    override fun handle(action: UserListAction) {
        when (action) {
            is UserListAction.SearchUsers                -> handleSearchUsers(action.value)
            is UserListAction.ClearSearchUsers           -> handleClearSearchUsers()
            is UserListAction.AddPendingSelection        -> handleSelectUser(action)
            is UserListAction.RemovePendingSelection     -> handleRemoveSelectedUser(action)
            UserListAction.ComputeMatrixToLinkForSharing -> handleShareMyMatrixToLink()
            is UserListAction.UpdateUserConsent          -> handleISUpdateConsent(action)
        }.exhaustive
    }

    private fun handleISUpdateConsent(action: UserListAction.UpdateUserConsent) {
        session.identityService().setUserConsent(action.consent)
        withState {
            identityServerUsersSearch.tryEmit(it.searchTerm)
        }
    }

    private fun handleSearchUsers(searchTerm: String) {
        setState {
            copy(
                    searchTerm = searchTerm
            )
        }
        if (searchTerm.isEmail().not()) {
            // if it's not an email reset to uninitialized
            // because the flow won't be triggered and result would stay
            setState {
                copy(
                        matchingEmail = Uninitialized
                )
            }
        }
        identityServerUsersSearch.tryEmit(searchTerm)
        knownUsersSearch.tryEmit(searchTerm)
        directoryUsersSearch.tryEmit(searchTerm)
    }

    private fun handleShareMyMatrixToLink() {
        session.permalinkService().createPermalink(session.myUserId)?.let {
            _viewEvents.post(UserListViewEvents.OpenShareMatrixToLink(it))
        }
    }

    private fun handleClearSearchUsers() {
        knownUsersSearch.tryEmit("")
        directoryUsersSearch.tryEmit("")
        identityServerUsersSearch.tryEmit("")
        setState {
            copy(searchTerm = "")
        }
    }

    private fun observeUsers() = withState { state ->
        identityServerUsersSearch
                .filter { it.isEmail() }
                .sample(300)
                .onEach { search ->
                    executeSearchEmail(search)
                }.launchIn(viewModelScope)

        knownUsersSearch
                .sample(300)
                .flowOn(Dispatchers.Main)
                .flatMapLatest { search ->
                    session.getPagedUsersLive(search, state.excludedUserIds).asFlow()
                }.execute {
                    copy(knownUsers = it)
                }

        directoryUsersSearch
                .debounce(300)
                .onEach { search ->
                    executeSearchDirectory(state, search)
                }.launchIn(viewModelScope)
    }

    private suspend fun executeSearchEmail(search: String) {
        suspend {
            val params = listOf(ThreePid.Email(search))
            val foundThreePid = tryOrNull {
                session.identityService().lookUp(params).firstOrNull()
            }
            if (foundThreePid == null) {
                null
            } else {
                try {
                    val json = session.getProfile(foundThreePid.matrixId)
                    ThreePidUser(
                            email = search,
                            user = User(
                                    userId = foundThreePid.matrixId,
                                    displayName = json[ProfileService.DISPLAY_NAME_KEY] as? String,
                                    avatarUrl = json[ProfileService.AVATAR_URL_KEY] as? String
                            )
                    )
                } catch (failure: Throwable) {
                    ThreePidUser(email = search, user = User(foundThreePid.matrixId))
                }
            }
        }.execute {
            copy(matchingEmail = it)
        }
    }

    private suspend fun executeSearchDirectory(state: UserListViewState, search: String) {
        suspend {
            if (search.isBlank()) {
                emptyList()
            } else {
                val searchResult = session
                        .searchUsersDirectory(search, 50, state.excludedUserIds.orEmpty())
                        .sortedBy { it.toMatrixItem().firstLetterOfDisplayName() }
                val userProfile = if (MatrixPatterns.isUserId(search)) {
                    val json = tryOrNull { session.getProfile(search) }
                    User(
                            userId = search,
                            displayName = json?.get(ProfileService.DISPLAY_NAME_KEY) as? String,
                            avatarUrl = json?.get(ProfileService.AVATAR_URL_KEY) as? String
                    )
                } else {
                    null
                }
                if (userProfile == null || searchResult.any { it.userId == userProfile.userId }) {
                    searchResult
                } else {
                    listOf(userProfile) + searchResult
                }
            }
        }.execute {
            copy(directoryUsers = it)
        }
    }

    private fun handleSelectUser(action: UserListAction.AddPendingSelection) = withState { state ->
        val selections = state.pendingSelections.toggle(action.pendingSelection, singleElement = state.singleSelection)
        setState { copy(pendingSelections = selections) }
    }

    private fun handleRemoveSelectedUser(action: UserListAction.RemovePendingSelection) = withState { state ->
        val selections = state.pendingSelections.minus(action.pendingSelection)
        setState { copy(pendingSelections = selections) }
    }
}
