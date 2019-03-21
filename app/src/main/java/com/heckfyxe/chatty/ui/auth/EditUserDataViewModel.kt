package com.heckfyxe.chatty.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.sendbird.android.SendBird
import com.sendbird.android.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.io.File

class EditUserDataViewModel : ViewModel(), KoinComponent {
    val currentUser = MutableLiveData<User>()
    val errors = MutableLiveData<Error>()
    val checkedNicknameLiveData = MutableLiveData<CheckedNickname>()

    private val userId: String by inject("uid")
    private val usersRef: CollectionReference by inject("users")

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
                            errors.postValue(Error(ErrorType.CHECK_NICKNAME, mapOf("nickname" to nickname)))
                    }
            }
        }
    }

    fun connectUser() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        SendBird.connect(userId) { user, exception ->
            if (exception != null) {
                errors.postValue(Error(ErrorType.CONNECT_USER))
                return@connect
            }

            currentUser.postValue(user)
        }
    }

    fun updateUserData(nickname: String, avatar: File, onSuccess: () -> Unit) {
        usersRef.document(userId).set(mapOf("nickname" to nickname), SetOptions.merge()).addOnCompleteListener {
            if (it.isSuccessful) {
                SendBird.updateCurrentUserInfoWithProfileImage(nickname, avatar) { e ->
                    if (e != null) {
                        errors.postValue(Error(ErrorType.UPDATE_USER_DATA))
                        return@updateCurrentUserInfoWithProfileImage
                    }

                    onSuccess()
                }
            } else {
                errors.postValue(Error(ErrorType.UPDATE_USER_DATA))
            }
        }
    }

    fun updateUserData(nickname: String, onSuccess: () -> Unit) {
        usersRef.document(userId).set(mapOf("nickname" to nickname), SetOptions.merge()).addOnCompleteListener {
            if (it.isSuccessful) {
                val currentUser = SendBird.getCurrentUser()
                SendBird.updateCurrentUserInfo(nickname, currentUser.profileUrl) { e ->
                    if (e != null) {
                        errors.postValue(Error(ErrorType.UPDATE_USER_DATA))
                        return@updateCurrentUserInfo
                    }

                    onSuccess()
                }
            } else {
                errors.postValue(Error(ErrorType.UPDATE_USER_DATA))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        job.cancel()
        checkingNicknameChannel.close()
    }

    class CheckedNickname(val nickname: String, val allowed: Boolean)

    class Error(val type: ErrorType, val extra: Map<Any, Any>? = null)

    enum class ErrorType {
        CONNECT_USER, UPDATE_USER_DATA, CHECK_NICKNAME
    }
}
