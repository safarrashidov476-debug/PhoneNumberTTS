package com.example.phonenumbertts

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.TtsSpan

/**
 * Matn ichidan turli raqam ketma-ketliklarini topib, TalkBack/TTS ularni
 * BITTA KATTA SON sifatida emas, balki har bir GURUHNI ALOHIDA SON sifatida
 * o'qishini ta'minlaydi.
 *
 * Misollar:
 *  - Telefon (9 xona, 2-3-2-2 guruh): 94 983 57 07
 *      -> "94" / "983" / "57" / "07" har biri alohida son sifatida o'qiladi
 *  - Xalqaro format: +998 94 983 57 07 -> yuqoridagi kabi, +998 ham qo'shiladi
 *  - Karta raqami: 9860 0825 ... -> 2 xonali guruhlarga bo'linadi:
 *      "98" "60" "08" "25" ... har biri alohida son sifatida o'qiladi
 *
 * "-lik" kabi qo'shimchalar ISHLATILMAYDI — faqat guruhlash to'g'ri bo'lishi kerak.
 */
object NumberFormatter {

    // Ixtiyoriy bo'shliq/tire bilan ajratilgan yoki ajratilmagan 9 xonali o'zbek raqami
    private val UZ_LOCAL_REGEX = Regex(
        "(?<!\\d)(\\d{2})[\\s-]?(\\d{3})[\\s-]?(\\d{2})[\\s-]?(\\d{2})(?!\\d)"
    )

    // +998 bilan boshlanuvchi xalqaro format
    private val UZ_INTL_REGEX = Regex(
        "\\+?998[\\s-]?(\\d{2})[\\s-]?(\\d{3})[\\s-]?(\\d{2})[\\s-]?(\\d{2})(?!\\d)"
    )

    // Karta raqamiga o'xshash uzun raqam ketma-ketligi (10 tadan ko'p xona,
    // bo'shliq/tire bilan yoki bo'lmasa ham) — 2 xonali guruhlarga bo'linadi
    private val LONG_DIGIT_SEQUENCE_REGEX = Regex(
        "(?<!\\d)(\\d[\\s-]?){10,19}\\d(?!\\d)"
    )

    /**
     * Berilgan matndagi raqamlarni aniqlaydi, guruhlarga ajratib qayta yozadi
     * va har bir guruhga CardinalBuilder TtsSpan biriktiradi — shu orqali TTS
     * dvigateli har bir guruhni alohida son sifatida talaffuz qiladi.
     */
    fun buildSpeechText(input: String): CharSequence {
        var result = input

        // Eng uzun/spetsifik patternlarni birinchi qayta ishlaymiz
        result = UZ_INTL_REGEX.replace(result) { m ->
            val g = m.groupValues
            "+998 ${g[1]} ${g[2]} ${g[3]} ${g[4]}"
        }

        result = UZ_LOCAL_REGEX.replace(result) { m ->
            val g = m.groupValues
            "${g[1]} ${g[2]} ${g[3]} ${g[4]}"
        }

        result = LONG_DIGIT_SEQUENCE_REGEX.replace(result) { m ->
            groupIntoPairs(m.value.replace(Regex("[\\s-]"), ""))
        }

        return annotateWithTtsSpans(result)
    }

    /** Uzluksiz raqam qatorini 2 xonali guruhlarga bo'ladi: "98600825" -> "98 60 08 25" */
    private fun groupIntoPairs(digits: String): String {
        return digits.chunked(2).joinToString(" ")
    }

    /**
     * Bo'shliq bilan ajratilgan har bir raqam guruhini CardinalBuilder bilan
     * belgilaydi — shu orqali har bir guruh mustaqil son sifatida o'qiladi va
     * TTS dvigateli qo'shni guruhlarni birlashtirib, yagona katta son qilib
     * o'qimaydi.
     */
    private fun annotateWithTtsSpans(text: String): CharSequence {
        val builder = SpannableStringBuilder(text)

        // "+998" dan tashqari barcha alohida raqam guruhlarini topamiz
        val groupRegex = Regex("(?<![+\\d])(\\d{1,4})(?!\\d)")
        for (match in groupRegex.findAll(text)) {
            val group = match.groups[1] ?: continue
            builder.setSpan(
                TtsSpan.CardinalBuilder(group.value).build(),
                group.range.first,
                group.range.last + 1,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE
            )
        }

        return builder
    }
}
