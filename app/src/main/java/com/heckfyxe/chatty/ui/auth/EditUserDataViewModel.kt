package com.heckfyxe.chatty.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.sendbird.android.SendBird
import com.sendbird.android.SendBirdException
import com.sendbird.android.User
import java.io.File

class EditUserDataViewModel : ViewModel() {
    val currentUser = MutableLiveData<User>()
    val errors = MutableLiveData<SendBirdException>()

    fun connectUser() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        SendBird.connect(userId) { user, exception ->
            if (exception != null) {
                errors.postValue(exception)
                return@connect
            }

            currentUser.postValue(user)
        }
    }

    fun updateUserData(nickname: String, avatar: File, onSuccess: () -> Unit) {
        SendBird.updateCurrentUserInfoWithProfileImage(nickname, avatar) {
            if (it != null) {
                errors.postValue(it)
                return@updateCurrentUserInfoWithProfileImage
            }

            onSuccess()
        }
    }

    fun updateUserData(nickname: String, onSuccess: () -> Unit) {
        object : Observer<User> {
            override fun onChanged(user: User) {
                SendBird.updateCurrentUserInfo(nickname, user.profileUrl) { e ->
                    if (e != null) {
                        errors.postValue(e)
                        currentUser.removeObserver(this)
                        return@updateCurrentUserInfo
                    }

                    onSuccess()
                    currentUser.removeObserver(this)
                }
            }
        }.let { currentUser.observeForever(it) }
    }
}
