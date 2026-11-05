package com.ntdpz.pie.feature.editor

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A session owns one recognizer. Destroying it invalidates callbacks before another session starts. */
internal class OnDeviceDictation(
    private val context: Context,
    private val onResult: (String) -> Unit,
) {
    var active by mutableStateOf(false)
        private set
    var finishing by mutableStateOf(false)
        private set
    var partial by mutableStateOf("")
        private set
    var message by mutableStateOf<String?>(null)
        private set
    private var recognizer: SpeechRecognizer? = null
    private var session = 0

    fun start() {
        if (active) return
        message = null
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            message = "เครื่องนี้ยังไม่มีบริการพิมพ์เสียงในเครื่อง ลองใช้ไมค์บนคีย์บอร์ด"
            return
        }
        val token = ++session
        active = true
        finishing = false
        partial = ""
        try {
            val engine = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recognizer = engine
            engine.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    if (token == session) finishing = true
                }
                override fun onError(error: Int) {
                    if (token != session) return
                    cancel()
                    message = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "เปิดสิทธิ์ไมโครโฟนเพื่อพิมพ์ด้วยเสียง"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "เครื่องยังไม่มีโมเดลเสียงภาษาไทย ลองใช้ไมค์บนคีย์บอร์ด"
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ยังไม่ได้ยินคำพูด ลองพูดใหม่"
                        else -> "ถอดเสียงไม่สำเร็จ ลองอีกครั้ง"
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (token != session) return
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    cancel()
                    if (text.isNotBlank()) onResult(text) else message = "ยังไม่ได้ยินคำพูด ลองพูดใหม่"
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (token == session) {
                        partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            })
        } catch (_: Exception) {
            cancel()
            message = "เปิดการพิมพ์เสียงในเครื่องไม่สำเร็จ ลองใช้ไมค์บนคีย์บอร์ด"
        }
    }

    fun permissionDenied() {
        message = "เปิดสิทธิ์ไมโครโฟนเพื่อพิมพ์ด้วยเสียง"
    }

    fun stop() {
        if (!active || finishing) return
        finishing = true
        recognizer?.stopListening()
    }

    fun cancel() {
        ++session
        val previous = recognizer
        recognizer = null
        active = false
        finishing = false
        partial = ""
        previous?.cancel()
        previous?.destroy()
    }

    fun timedOut() {
        cancel()
        message = "รอผลเสียงนานเกินไป ลองอีกครั้ง"
    }
}
