package com.ntdpz.pie.di

import android.content.Context
import com.ntdpz.pie.core.data.DiaryDataFactory
import com.ntdpz.pie.core.domain.diary.DiaryWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val diaryRepository = DiaryDataFactory.createRepository(context)
    val diaryWriter = DiaryWriter(diaryRepository, applicationScope, clock)
}
