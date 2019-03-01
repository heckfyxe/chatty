package com.heckfyxe.chatty.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import com.sendbird.android.SendBirdException
import com.sendbird.android.User

class MainViewModel : ViewModel() {

    val currentUser = MutableLiveData<User>()

    val errors = MutableLiveData<SendBirdException>()

    val chats = MutableLiveData<List<GroupChannel>>()

    fun connectUser() {
        val user = FirebaseAuth.getInstance().currentUser!!

        SendBird.connect(user.uid) { sendBirdUser, e ->
            if (e != null) {
                errors.postValue(e)
                return@connect
            }

            currentUser.postValue(sendBirdUser)
        }
    }

    fun loadChats() {
        GroupChannel.createMyGroupChannelListQuery().next { groups, e ->
            if (e != null) {
                errors.postValue(e)
                return@next
            }

            chats.postValue(groups)
        }
    }

    fun logOut(action: () -> Unit) {
        SendBird.disconnect {
            FirebaseAuth.getInstance().signOut()
            action()
        }
    }
}
