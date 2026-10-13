package fr.visionmaxread

import android.content.Context
import java.util.function.BooleanSupplier
import java.util.function.Consumer

/** Stable speech abstraction: UI code must not know Sherpa/Piper details. */
interface SpeechEngine {
    fun initialize(context: Context, status: Consumer<String>? = null)
    fun warmUp(context: Context, cancelled: BooleanSupplier = BooleanSupplier { false })
    fun speak(context: Context, text: String, speed: Float, cancelled: BooleanSupplier, status: Consumer<String>)
    fun stop()
    fun release()
    fun isReady(): Boolean
}
