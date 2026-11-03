package com.ntdpz.pie.di

import android.content.Context
import com.ntdpz.pie.core.data.DiaryDataFactory
import com.ntdpz.pie.core.domain.diary.DiaryWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import com.ntdpz.pie.core.data.settings.LocalSettingsRepository
import com.ntdpz.pie.core.data.reminder.AndroidReminders
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settingsRepository = LocalSettingsRepository(context)
    val reminders = AndroidReminders(context)
    private val data = DiaryDataFactory.create(context, settingsRepository)
    val diaryRepository = data.repository
    val mediaRepository = data.media
    val backupRepository = data.backup
    val mergeImportRepository = data.merge
    val diaryWriter = DiaryWriter(diaryRepository, applicationScope, clock)

    init {
        applicationScope.launch {
            settingsRepository.settings.map { it.reminder }.distinctUntilChanged().collect(reminders::schedule)
        }
    }
}
