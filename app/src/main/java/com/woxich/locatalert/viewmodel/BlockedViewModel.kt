package com.woxich.locatalert.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.woxich.locatalert.BlocklistChangedEvent
import com.woxich.locatalert.R
import com.woxich.locatalert.SnackbarDispatcher
import com.woxich.locatalert.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@HiltViewModel
class BlockedViewModel @Inject constructor(
    val snackbarDispatcher: SnackbarDispatcher,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
    private val pictureRepository: PictureRepository
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _currentUser = MutableStateFlow(User())
    val currentUser: StateFlow<User> = _currentUser

    private val _blockedUsers = MutableStateFlow(listOf<DisplayUser>())
    val blockedUsers: StateFlow<List<DisplayUser>> = _blockedUsers

    init {
        EventBus.getDefault().register(this)
        getCurrentUserAndBlocks()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    /**
     * Called when another component signaled that the user's block list changed.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBlockListChangedEvent(event: BlocklistChangedEvent) {
        //update state
        getCurrentUserAndBlocks()
    }

    private fun getCurrentUserAndBlocks() {
        _loading.value = true
        userRepository.getCurrentUser().addOnCompleteListener { currentUserResult ->
            if(currentUserResult.isSuccessful) {
                _currentUser.value = currentUserResult.result!!.toObjects(User::class.java)[0]
                //get contact users
                if(_currentUser.value.blockedUsers.isNotEmpty()) {
                    userRepository.getUsersByUid(currentUser.value.blockedUsers).addOnCompleteListener { blocksQueryResult ->
                        if(blocksQueryResult.isSuccessful && blocksQueryResult.result != null) {
                            //USER objects queried.
                            val rawData = blocksQueryResult.result!!.toObjects(User::class.java)
                            //start async operation ot convert them into display user objects
                            val onCompletion = { displayUsers: List<DisplayUser> ->
                                _loading.value = false
                                _blockedUsers.value = displayUsers.sortedBy { it.displayName }
                            }
                            createDisplayUsers(pictureRepository, rawData, onCompletion)
                        } else {
                            _loading.value = false
                            //failed to get blocklist
                            showBlocklistLoadErrorSnackbar()
                        }
                    }
                } else {
                    _loading.value = false
                    //no users to query
                    _blockedUsers.value = listOf()
                }
            } else {
                _loading.value = false
                //failed to get current user
                showBlocklistLoadErrorSnackbar()
            }
        }
    }

    /**
     * Called when the user unblocks a blocked user.
     * @param position The position of the unblocked user in [blockedUsers]
     */
    fun onUnblockClicked(position: Int) {
        val unblockedUser = blockedUsers.value[position]
        val unblockUid = unblockedUser.uid
        val unblockDisplayName = unblockedUser.displayName
        userRepository.removeUserBlock(currentUser.value, unblockUid)
            ?.addOnCompleteListener { unblockResult ->
                if(unblockResult.isSuccessful) {
                    //the database was updated, now update the state
                    _currentUser.value = currentUser.value.copy(blockedUsers = currentUser.value.blockedUsers - unblockUid)
                    //update list
                    val mutableBlockList = mutableListOf<DisplayUser>()
                    var indexOfUnblocked = 0
                    blockedUsers.value.forEachIndexed { index, blockedUser ->
                        if(blockedUser.uid == unblockUid) {
                            //this was the user who was unblocked
                            indexOfUnblocked = index
                        } else {
                            //this was not the unblocked, add it
                            mutableBlockList.add(blockedUser)
                        }
                    }
                    //reassign the block list state
                    _blockedUsers.value = mutableBlockList.toList()
                    //show success snackbar with re-block option
                    snackbarDispatcher.setSnackbarMessage(context.getString(R.string.home_unblock_success, unblockDisplayName))
                    snackbarDispatcher.setSnackbarLabel(context.getString(R.string.undo))
                    snackbarDispatcher.setSnackbarAction { reBlockUser(indexOfUnblocked, unblockedUser) } //retry action
                    snackbarDispatcher.showSnackbar()
                } else {
                    val message = context.getString(R.string.home_unblock_failed)
                    snackbarDispatcher.createOnlyMessageSnackbar(message)
                    snackbarDispatcher.showSnackbar()
                }
            }
    }

    private fun reBlockUser(indexOfUnblocked: Int, unblockedUser: DisplayUser) {
        val mutableBlockList = mutableListOf<DisplayUser>()
        mutableBlockList.addAll(blockedUsers.value) //this does not include unblocked user anymore
        //re add it
        mutableBlockList.add(indexOfUnblocked, unblockedUser)
        //update state
        _blockedUsers.value = mutableBlockList.toList()
        //send to database
        userRepository.addUserBlock(user = currentUser.value, unblockedUser.uid)
    }

    private fun showBlocklistLoadErrorSnackbar() {
        snackbarDispatcher.setSnackbarMessage(context.getString(R.string.home_block_error))
        snackbarDispatcher.setSnackbarLabel(context.getString(R.string.retry))
        snackbarDispatcher.setSnackbarAction { getCurrentUserAndBlocks() } //retry action
        snackbarDispatcher.showSnackbar()
    }

}