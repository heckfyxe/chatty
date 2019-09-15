package com.heckfyxe.chatty.remote

import com.heckfyxe.chatty.room.User
import com.heckfyxe.chatty.util.sendbird.saveOnDevice
import com.heckfyxe.chatty.util.sendbird.toRoomUser
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.select

class SendBirdApi {
    private val scope = CoroutineScope(Dispatchers.Default)

    suspend fun connect(userId: String): User {
        val result = Channel<User>()
        val error = Channel<Exception>()
        SendBird.connect(userId) { user, e ->
            scope.launch {
                if (e != null) {
                    error.send(e)
                    return@launch
                }

                result.send(user.toRoomUser())
            }
        }
        return select {
            result.onReceive {
                result.close()
                error.close()
                it
            }
            error.onReceive {
                result.close()
                error.close()
                throw it
            }
        }
    }

    suspend fun getChannels(): ReceiveChannel<List<GroupChannel>> {
        val currentUser = SendBird.getCurrentUser()
        connect(currentUser.userId)
        val result = Channel<List<GroupChannel>>()
        GroupChannel.createMyGroupChannelListQuery().next { channels, e ->
            scope.launch {
                if (e != null) {
                    result.close(e)
                    return@launch
                }
                if (channels.isEmpty()) {
                    result.close()
                    return@launch
                }
                val dialogs = channels.map {
                    async(Dispatchers.IO) {
                        it.saveOnDevice()
                        it
                    }
                }
                result.send(dialogs.awaitAll())
            }
        }
        return result
    }

    suspend fun getChannel(channelUrl: String): GroupChannel {
        val result = Channel<GroupChannel>()
        val error = Channel<Exception>()
        GroupChannel.getChannel(channelUrl) { channel, e ->
            scope.launch {
                if (e != null) {
                    error.send(e)
                    return@launch
                }

                result.send(channel)
            }
        }
        return select {
            result.onReceive {
                result.close()
                error.close()
                it
            }
            error.onReceive {
                result.close()
                error.close()
                throw it
            }
        }
    }
}