package com.heckfyxe.chatty.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.repository.EditUserDataRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File

enum class ErrorType {
    UPDATE_USER_DATA, CHECK_NICKNAME
}

sealed class Status
object Initializing : Status()
object CheckingNickname : Status()
data class NicknameReady(val allowed: Boolean) : Status()
object UpdatingData : Status()
object DataUpdated : Status()
object Finish : Status()
data class Error(val type: ErrorType) : Status()

private const val BROKEN_IMAGE_URL = "error!"

class EditUserDataViewModel(
    application: Application,
    private val repository: EditUserDataRepository
) : AndroidViewModel(application), KoinComponent {

    private val firebaseAuth: FirebaseAuth by inject()
    private val phoneNumber: String by lazy { firebaseAuth.currentUser!!.phoneNumber!! }

    private val resources = application.resources
    private val userId: String by inject(KOIN_USER_ID)

    private val _status = MutableLiveData<Status>()
    val status: LiveData<Status> = _status

    private val _takePhotoEvent = MutableLiveData<Boolean>()
    val takePhotoEvent: LiveData<Boolean> = _takePhotoEvent

    private val isPhotoTaken = MutableLiveData<Boolean>()

    private val _profileImage = MutableLiveData<Any>()
    val profileImage: LiveData<Any> = _profileImage

    val helperText = Transformations.map(_status) { status ->
        when (status) {
            is NicknameReady -> {
                if (status.allowed)
                    resources.getString(R.string.allowed)
                else
                    resources.getString(R.string.the_nickname_is_already_taken)
            }
            is Error -> {
                resources.getString(R.string.error)
            }
            else -> null
        }
    }

    val helperTextColor = Transformations.map(_status) { status ->
        when (status) {
            is NicknameReady -> {
                if (status.allowed)
                    R.color.green
                else
                    R.color.warning
            }
            is Error -> {
                R.color.error
            }
            else -> null
        }
    }

    val isProgress = Transformations.map(_status) { status ->
        status is CheckingNickname
    }

    val nicknameReady = Transformations.map(_status) { status ->
        status is NicknameReady
    }

    private val _nickname = MutableLiveData<String>()

    private val checkingNicknameChannel = Channel<String>(Channel.CONFLATED)

    fun init() {
        _status.value = Initializing
        viewModelScope.launch {
            launch {
                try {
                    val avatarUrl = repository.getAvatarUrl()
                    if (_profileImage.value == null)
                        _profileImage.value = avatarUrl
                } catch (e: Exception) {
                    _profileImage.value = BROKEN_IMAGE_URL
                }
            }
            for (nickname in checkingNicknameChannel) {
                delay(300)

                if (_nickname.value != nickname) continue

                launch {
                    try {
                        val isAllowed = repository.checkNickname(nickname)
                        if (_nickname.value == nickname) {
                            _status.value = NicknameReady(isAllowed)
                        }
                    } catch (e: Exception) {
                        if (_nickname.value == nickname) {
                            Log.w("EditUserDataViewModel", "error", e)
                            _status.value = Error(ErrorType.CHECK_NICKNAME)
                        }
                    }
                }
            }
        }
    }

    fun checkNickname(nickname: String) {
        if (_nickname.value == nickname) return

        _nickname.value = nickname
        _status.value = CheckingNickname
        viewModelScope.launch {
            checkingNicknameChannel.send(nickname)
        }
    }

    fun onDataUpdated() {
        _status.value = Finish
    }

    fun takePhoto() {
        _takePhotoEvent.value = true
    }

    fun onTakePhoto() {
        _takePhotoEvent.value = false
    }

    fun onPhotoTaken(photo: File) {
        isPhotoTaken.value = true
        _profileImage.value = photo
    }

    fun updateUserData() {
        if (_status.value == UpdatingData) return
        val nickname = _nickname.value ?: return
        _status.value = UpdatingData
        viewModelScope.launch {
            try {
                when (_profileImage.value) {
                    is File -> repository.updateUserInfo(
                        userId,
                        nickname,
                        phoneNumber,
                        _profileImage.value as File
                    )
                    else -> repository.updateUserInfo(userId, nickname, phoneNumber)
                }
                _status.value = DataUpdated
            } catch (e: Exception) {
                _status.value = Error(ErrorType.UPDATE_USER_DATA)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        checkingNicknameChannel.close()
    }
}
