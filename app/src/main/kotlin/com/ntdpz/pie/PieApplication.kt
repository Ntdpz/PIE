package com.ntdpz.pie

import android.app.Application
import com.ntdpz.pie.di.AppContainer

class PieApplication : Application() {
    val container by lazy { AppContainer(applicationContext) }
}
