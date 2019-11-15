package com.heckfyxe.chatty.ui.friends

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sendbird.android.GroupChannel
import com.sendbird.android.GroupChannelParams
import com.sendbird.android.SendBird
import com.sendbird.android.User

class FriendsViewModel : ViewModel() {

    val friends = MutableLiveData<List<User>>()
    val errors = MutableLiveData<Exception>()

    private var isLoading = false

    fun loadFriends() {
        if (isLoading)
            return

        isLoading = true

        SendBird.createFriendListQuery().next { friendList, e ->
            isLoading = false

            if (e != null) {
                errors.postValue(e)
                return@next
            }

            friends.postValue(friendList)
        }
    }

    fun createChannel(user: User, onSuccess: (GroupChannel) -> Unit) {
        GroupChannel.createDistinctChannelIfNotExist(GroupChannelParams().addUser(user)) { channel, _, e ->
            if (e != null) {
                errors.postValue(e)
                return@createDistinctChannelIfNotExist
            }

            onSuccess(channel)
        }
    }
}
