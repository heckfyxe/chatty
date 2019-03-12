package com.heckfyxe.chatty.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sendbird.android.SendBird
import com.sendbird.android.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File

class EditUserDataViewModel : ViewModel() {
    val currentUser = MutableLiveData<User>()
    val errors = MutableLiveData<Exception>()
    val checkedNicknameLiveData = MutableLiveData<CheckedNickname>()

    private val userId = FirebaseAuth.getInstance().currentUser!!.uid
    private val usersRef = FirebaseFirestore.getInstance().collection("users")

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    val checkingNicknameChannel = Channel<String>(Channel.CONFLATED)

    fun init() {
        scope.launch {
            for (nickname in checkingNicknameChannel) {
                usersRef
                    .whereEqualTo("nickname", nickname)
                    .limit(1)
                    .get()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            val isAllowed =
                                it.result == null || it.result!!.isEmpty || it.result!!.documents.first().id == userId
                            checkedNicknameLiveData.postValue(CheckedNickname(nickname, isAllowed))
                        } else
                            errors.postValue(it.exception)
                    }
            }
        }
    }

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
        usersRef.document(userId).set(mapOf("nickname" to nickname)).addOnCompleteListener {
            if (it.isSuccessful) {
                SendBird.updateCurrentUserInfoWithProfileImage(nickname, avatar) { e ->
                    if (e != null) {
                        errors.postValue(e)
                        return@updateCurrentUserInfoWithProfileImage
                    }

                    onSuccess()
                }
            } else {
                errors.postValue(it.exception)
            }
        }
    }

    fun updateUserData(nickname: String, onSuccess: () -> Unit) {
        usersRef.document(userId).set(mapOf("nickname" to nickname)).addOnCompleteListener {
            if (it.isSuccessful) {
                val currentUser = SendBird.getCurrentUser()
                SendBird.updateCurrentUserInfo(nickname, currentUser.profileUrl) { e ->
                    if (e != null) {
                        errors.postValue(e)
                        return@updateCurrentUserInfo
                    }

                    onSuccess()
                }
            } else {
                errors.postValue(it.exception)
            }
        }

    }

    override fun onCleared() {
        super.onCleared()

        job.cancel()
    }

    class CheckedNickname(val nickname: String, val allowed: Boolean)
}
