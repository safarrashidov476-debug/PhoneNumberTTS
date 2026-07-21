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

class CustomTtsService : TextToSpeechService() {

    companion object {
        private const val TAG = "CustomTtsService"
        private const val RHVOICE_PACKAGE = "com.github.olga_yakovleva.rhvoice.android"
        private const val SAMPLE_RATE = 22050
    }

    private var rhvoiceTts: TextToSpeech? = null
    private var rhvoiceReady = false

    override fun onCreate() {
        super.onCreate()
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
        val formatted = NumberFormatter.buildSpeechText(originalText)

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

        latch.await(10, TimeUnit.SECONDS)

        if (!success || !outFile.exists()) {
            Log.e(TAG, "Audio fayl yaratilmadi")
            callback.error()
            return
        }

        streamWavFileToCallback(outFile, callback)
        outFile.delete()
    }

    private fun streamWavFileToCallback(file: File, callback: SynthesisCallback) {
        try {
            val bytes = file.readBytes()
            if (bytes.size < 12 || String(bytes, 0, 4, Charsets.US_ASCII) != "RIFF") {
                Log.e(TAG, "Fayl to'g'ri WAV formatida emas")
                callback.error()
                return
            }

            var pos = 12
            var sampleRate = SAMPLE_RATE
            var channels = 1
            var bitsPerSample = 16
            var dataStart = -1
            var dataSize = 0

            while (pos + 8 <= bytes.size) {
                val chunkId = String(bytes, pos, 4, Charsets.US_ASCII)
                val chunkSize = (bytes[pos + 4].toInt() and 0xFF) or
                        ((bytes[pos + 5].toInt() and 0xFF) shl 8) or
                        ((bytes[pos + 6].toInt() and 0xFF) shl 16) or
                        ((bytes[pos + 7].toInt() and 0xFF) shl 24)
                val chunkDataStart = pos + 8

                if (chunkId == "fmt ") {
                    channels = (bytes[chunkDataStart + 2].toInt() and 0xFF) or
                            ((bytes[chunkDataStart + 3].toInt() and 0xFF) shl 8)
                    sampleRate = (bytes[chunkDataStart + 4].toInt() and 0xFF) or
                            ((bytes[chunkDataStart + 5].toInt() and 0xFF) shl 8) or
                            ((bytes[chunkDataStart + 6].toInt() and 0xFF) shl 16) or
                            ((bytes[chunkDataStart + 7].toInt() and 0xFF) shl 24)
                    bitsPerSample = (bytes[chunkDataStart + 14].toInt() and 0xFF) or
                            ((bytes[chunkDataStart + 15].toInt() and 0xFF) shl 8)
                } else if (chunkId == "data") {
                    dataStart = chunkDataStart
                    dataSize = chunkSize
                    break
                }

                pos = chunkDataStart + chunkSize + (chunkSize % 2)
            }

            if (dataStart < 0) {
                Log.e(TAG, "WAV faylida 'data' bo'limi topilmadi")
                callback.error()
                return
            }

            val encoding = if (bitsPerSample == 8) {
                android.media.AudioFormat.ENCODING_PCM_8BIT
            } else {
                android.media.AudioFormat.ENCODING_PCM_16BIT
            }

            callback.start(sampleRate, encoding, channels)

            val maxChunk = callback.maxBufferSize
            var offset = dataStart
            val end = minOf(dataStart + dataSize, bytes.size)
            while (offset < end) {
                val len = minOf(maxChunk, end - offset)
                callback.audioAvailable(bytes, offset, len)
                offset += len
            }
            callback.done()
        } catch (e: Exception) {
            Log.e(TAG, "WAV faylni o'qishda xato: ${e.message}")
            callback.error()
        }
    }
}
