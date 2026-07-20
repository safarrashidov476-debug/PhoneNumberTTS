# Raqam formatlovchi TTS (RHVoice uchun)

## Nima qiladi
Bu ilova alohida TTS dvigateli sifatida ishlaydi. TalkBack yoki istalgan ilova
biror matnni "aytmoqchi" bo'lganda, tizim endi shu dvigatelga murojaat qiladi.
Dvigatel:

1. Kiruvchi matndan telefon/karta raqamlarini aniqlaydi (`NumberFormatter.kt`)
2. Ularni to'g'ri guruhlarga ajratadi:
   - `94 983 57 07` -> har bir guruh (94, 983, 57, 07) alohida son sifatida
   - `+998 94 983 57 07` -> xuddi shunday, +998 bilan
   - Karta raqami (10-19 xonali uzun ketma-ketlik) -> 2 xonali guruhlarga bo'linadi
     (masalan `98600825` -> "98 60 08 25")
3. Formatlangan matnni **RHVoice** dvigateliga (`com.github.olga_yakovleva.rhvoice.android`)
   yuboradi va undan kelgan audio oqimni to'g'ridan-to'g'ri TalkBackka uzatadi.

## Talablar
- Android Studio (Koala yoki undan yangi)
- Qurilmada **RHVoice** ilovasi o'rnatilgan va kerakli til/ovoz yuklab olingan bo'lishi kerak
  (F-Droid yoki Google Play orqali)

## O'rnatish qadamlari
1. Loyihani Android Studio orqali oching (`File -> Open` -> ushbu papkani tanlang)
2. `Build -> Make Project` orqali build qiling
3. Qurilmaga o'rnating (`Run` tugmasi yoki APK export qilib qo'lda o'rnating)
4. Ilovani oching, "TTS sozlamalarini ochish" tugmasini bosing
5. Ochilgan sozlamalarda **"Afzal ko'rilgan dvigatel"** (Preferred engine) bo'limidan
   **"Raqam formatlovchi TTS"** ni tanlang
6. TalkBack endi shu dvigatel orqali gapiradi — u esa orqa fonda RHVoice'dan foydalanadi

## Diqqat qilinadigan joylar
- Agar RHVoice ba'zi qurilmalarda tizim TTS ro'yxatida ko'rinmasa (ma'lum muammo),
  RHVoice ilovasining o'zi to'g'ridan-to'g'ri o'rnatilgan va kamida bitta ovoz
  yuklab olingan bo'lishini tekshiring.
- `CustomTtsService.kt` ichidagi `RHVOICE_PACKAGE` konstantasi RHVoice'ning
  standart paket nomiga mos keladi. Agar boshqa build (masalan, F-Droid'dan
  boshqacha nom bilan) ishlatilsa, shu qatorni moslashtiring.
- Formatlash qoidalarini o'zgartirish uchun faqat `NumberFormatter.kt` faylini
  tahrirlash kifoya — qolgan qism (TTS bilan bog'lanish, audio uzatish) o'zgarishsiz qoladi.

## Fayllar tuzilishi
```
app/src/main/java/com/example/phonenumbertts/
├── NumberFormatter.kt   -> raqamlarni aniqlash va guruhlash mantig'i
├── CustomTtsService.kt  -> tizim TTS xizmati, RHVoice bilan bog'lanish
└── MainActivity.kt      -> sozlamalarga tez o'tish uchun oddiy ekran
```
