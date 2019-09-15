package com.heckfyxe.chatty.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.sendbird.android.SendBird
import com.sendbird.android.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject

class EditUserDataViewModel : ViewModel(), KoinComponent {
    val currentUser = MutableLiveData<User>()
    val errors = MutableLiveData<Error>()
    val checkedNicknameLiveData = MutableLiveData<CheckedNickname>()

    private val currentUserPhoneNumber = get<FirebaseAuth>().currentUser!!.phoneNumber!!
    private val userId: String by inject(KOIN_USER_ID)
    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

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

//    fun updateUserData(nickname: String, avatar: File, onSuccess: () -> Unit) {
//        usersRef.document(userId).set(mapOf("nickname" to nickname), SetOptions.merge()).addOnCompleteListener {
//            if (it.isSuccessful) {
//                SendBird.updateCurrentUserInfoWithProfileImage(nickname, avatar) { e ->
//                    if (e != null) {
//                        errors.postValue(Error(ErrorType.UPDATE_USER_DATA))
//                        return@updateCurrentUserInfoWithProfileImage
//                    }
//
//                    onSuccess()
//                }
//            } else {
//                errors.postValue(Error(ErrorType.UPDATE_USER_DATA))
//            }
//        }
//    }

    fun updateUserData(nickname: String, onSuccess: () -> Unit) {
        usersRef.document(userId).set(mapOf("nickname" to nickname), SetOptions.merge()).addOnCompleteListener {
            if (it.isSuccessful) {
                val currentUser = SendBird.getCurrentUser()
                val channel = Channel<Boolean>(2)
                scope.launch {
                    updatePhoneNumber(currentUser, channel)
                    updateNickname(currentUser, nickname, channel)

                    var isSuccessful = true
                    repeat(2) {
                        isSuccessful = isSuccessful && channel.receive()
                    }
                    channel.close()

                    if (isSuccessful)
                        onSuccess()
                }
            } else {
                errors.postValue(Error(ErrorType.UPDATE_USER_DATA))
            }
        }
    }

    private suspend fun updatePhoneNumber(currentUser: User, channel: SendChannel<Boolean>) {
        currentUser.createMetaData(mapOf("phoneNumber" to currentUserPhoneNumber)) { _, e ->
            scope.launch {
                if (e != null) {
                    errors.postValue(Error(ErrorType.UPDATE_USER_DATA))
                    channel.send(false)
                    return@launch
                }
                channel.send(true)
            }
        }
    }

    private suspend fun updateNickname(currentUser: User, nickname: String, channel: SendChannel<Boolean>) {
        SendBird.updateCurrentUserInfo(nickname, currentUser.profileUrl) { e ->
            scope.launch {
                if (e != null) {
                    errors.postValue(Error(ErrorType.UPDATE_USER_DATA))
                    channel.send(false)
                    return@launch
                }

                channel.send(true)
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
