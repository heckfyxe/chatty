package com.heckfyxe.chatty.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.util.sendbird.toDomain
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

class EditUserDataRepository(
    private val sendBirdApi: SendBirdApi,
    private val usersRef: CollectionReference
) {

    suspend fun connect() =
        sendBirdApi.connect().toDomain()

    suspend fun checkNickname(nickname: String): Boolean = suspendCancellableCoroutine { cont ->
        usersRef.whereEqualTo("nickname", nickname).limit(1).get(Source.SERVER)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    cont.cancel(it.exception)
                    return@addOnCompleteListener
                }
                cont.resume(it.result == null || it.result!!.isEmpty)
            }
    }

    suspend fun updateUserInfo(userId: String, nickname: String) {
        sendBirdApi.updateNickname(nickname)
        updateNicknameFirebase(userId, nickname)
    }

    suspend fun updateUserInfo(userId: String, nickname: String, avatarImage: File) {
        sendBirdApi.updateNicknameWithAvatarImage(nickname, avatarImage)
        updateNicknameFirebase(userId, nickname)
    }

    private suspend fun updateNicknameFirebase(userId: String, nickname: String) =
        suspendCancellableCoroutine<Unit> { cont ->
            usersRef.document(userId).set(mapOf("nickname" to nickname), SetOptions.merge())
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        cont.resume(Unit)
                    } else {
                        cont.cancel(it.exception)
                    }
                }
        }
}
