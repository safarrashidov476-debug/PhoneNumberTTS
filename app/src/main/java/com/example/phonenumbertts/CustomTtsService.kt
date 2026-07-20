package com.example.phonenumbertts

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Bu xizmat tizimning TTS dvigateli sifatida ro'yxatdan o'tadi.
 * TalkBack yoki istalgan ilova gapirmoqchi bo'lgan matnni shu xizmat qabul qiladi,
 * ichidagi raqamlarni (telefon/karta) to'g'ri guruhlarga ajratib qayta yozadi,
 * so'ngra haqiqiy ovoz sintezini RHVoice dvigateliga topshiradi.
 */
class CustomTtsService : TextToSpeechService() {

    companion object {
        private const val TAG = "CustomTtsService"
        // RHVoice'ning Android paket nomi
        private const val RHVOICE_PACKAGE = "com.github.olga_yakovleva.rhvoice.android"
        private const val SAMPLE_RATE = 22050
        private const val CHANNELS = android.media.AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING = android.media.AudioFormat.ENCODING_PCM_16BIT
    }

    private var rhvoiceTts: TextToSpeech? = null
    private var rhvoiceReady = false

    override fun onCreate() {
        super.onCreate()
        // RHVoice dvigateliga ichki mijoz sifatida ulanamiz
        rhvoiceTts = TextToSpeech(this, { status ->
            rhvoiceReady = (status == TextToSpeech.SUCCESS)
            if (!rhvoiceReady) {
                Log.e(TAG, "RHVoice dvigateliga ulanib bo'lmadi. RHVoice o'rnatilganini tekshiring.")
            }
        }, RHVOICE_PACKAGE)
    }

    override fun onDestroy() {
        rhvoiceTts?.shutdown()
        super.onDestroy()
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        return TextToSpeech.LANG_AVAILABLE
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf("uzb", "UZB", "")
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return TextToSpeech.LANG_AVAILABLE
    }

    override fun onStop() {
        rhvoiceTts?.stop()
    }

    override fun onGetVoices(): MutableList<Voice> {
        return mutableListOf()
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val engine = rhvoiceTts
        if (engine == null || !rhvoiceReady) {
            Log.e(TAG, "RHVoice hali tayyor emas")
            callback.error()
            return
        }

        val originalText = request.charSequenceText?.toString() ?: request.text ?: ""
        // Raqamlarni to'g'ri guruhlarga ajratib, TtsSpan bilan belgilaymiz
        val formatted = NumberFormatter.buildSpeechText(originalText)

        // RHVoice orqali audio faylga sintez qilamiz, keyin uni callback'ga uzatamiz
        val outFile = File(cacheDir, "tts_${System.currentTimeMillis()}.wav")
        val latch = CountDownLatch(1)
        var success = false

        val utteranceId = "utt_${System.currentTimeMillis()}"
        engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                success = true
                latch.countDown()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                success = false
                latch.countDown()
            }
        })

        val params = android.os.Bundle()
        val result = engine.synthesizeToFile(formatted, params, outFile, utteranceId)

        if (result != TextToSpeech.SUCCESS) {
            Log.e(TAG, "synthesizeToFile chaqiruvi muvaffaqiyatsiz tugadi")
            callback.error()
            return
        }

        // RHVoice faylga yozib bo'lguncha kutamiz (maksimum 10 soniya)
        latch.await(10, TimeUnit.SECONDS)

        if (!success || !outFile.exists()) {
            Log.e(TAG, "Audio fayl yaratilmadi")
            callback.error()
            return
        }

        streamWavFileToCallback(outFile, callback)
        outFile.delete()
    }

    /** WAV faylni o'qib, PCM ma'lumotlarini TTS callback orqali qism-qism uzatadi */
    private fun streamWavFileToCallback(file: File, callback: SynthesisCallback) {
        try {
            file.inputStream().use { input ->
                // WAV sarlavhasi (44 bayt) ni o'tkazib yuboramiz
                val header = ByteArray(44)
                input.read(header)

                callback.start(SAMPLE_RATE, ENCODING, 1)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } > 0) {
                    callback.audioAvailable(buffer, 0, bytesRead)
                }
                callback.done()
            }
        } catch (e: Exception) {
            Log.e(TAG, "WAV faylni o'qishda xato: ${e.message}")
            callback.error()
        }
    }
}
