package com.example.phonenumbertts

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val info = TextView(this).apply {
            text = "Bu ilova o'zi ovoz chiqarmaydi.\n\n" +
                    "U TalkBack/tizim uchun \"Raqam formatlovchi TTS\" nomli dvigatel bo'lib ishlaydi: " +
                    "matndagi telefon va karta raqamlarini to'g'ri guruhlarga ajratib, " +
                    "keyin RHVoice orqali aytadi.\n\n" +
                    "Ishga tushirish uchun pastdagi tugmani bosing va ochilgan sozlamalarda " +
                    "\"Afzal ko'rilgan dvigatel\" qismidan ushbu ilovani tanlang."
            textSize = 16f
        }

        val openSettingsButton = Button(this).apply {
            text = "TTS sozlamalarini ochish"
            setOnClickListener {
                startActivity(Intent("com.android.settings.TTS_SETTINGS"))
            }
        }

        layout.addView(info)
        layout.addView(openSettingsButton)
        setContentView(layout)
    }
}
