package com.heckfyxe.chatty.util.sendbird

import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.koin.userScope

internal val currentUserId: String
    get() = userScope.get(KOIN_USER_ID)