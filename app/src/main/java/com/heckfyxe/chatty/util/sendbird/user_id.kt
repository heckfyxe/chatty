package com.heckfyxe.chatty.util.sendbird

import com.heckfyxe.chatty.koin.KOIN_SCOPE_USER
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.scope.Scope

private val koin: Koin by lazy { GlobalContext.get().koin }
private val userScope: Scope by lazy { koin.getScope(KOIN_SCOPE_USER.value) }
internal val currentUserId: String by lazy { userScope.get<String>(KOIN_USER_ID) }