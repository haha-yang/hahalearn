package com.haha.mviFrame.main

import com.haha.baseui.mvi.IMviIntent

sealed interface MainIntent : IMviIntent {
    data object FetchUser : MainIntent
}
