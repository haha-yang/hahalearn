package com.haha.mviFrame.data

import com.haha.baseui.mvi.IMviIntent

sealed interface DataIntent : IMviIntent {
    data class RequestData(val id: String) : DataIntent
    data class RequestGuideInfo(val sn8: String) : DataIntent
}
