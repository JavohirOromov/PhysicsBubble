# PhysicsBubble

Android uchun **Jetpack Compose** asosida yozilgan interaktiv “pufak” (bubble) animatsiyasi. Foydalanuvchi pufakni vertikal tortadi; harakat fizikaga o‘xshash deformatsiya, radius o‘zgarishi va **AGSL RuntimeShader** orqali kinematik linza effekti bilan jonlantiriladi. Yorug‘ / qorong‘i mavzu tugmasi va gradient fon mavjud.

## Imkoniyatlar

- Vertikal tortish: pufak pastdan yuqoriga progress bo‘yicha harakatlanadi, radius interpolatsiya qilinadi.
- Deformatsiya va “pop” animatsiyalari (`Animatable`).
- **RuntimeShader** (Android 13+): fon va shader effektlari; qo‘llab-quvvatlanmagan qurilmalar uchun soddalashtirilgan **Canvas** fallback.
- **Material 3**, edge-to-edge, status bar rangi mavzuga moslashadi.

## Texnologiyalar

- Kotlin, Jetpack Compose, Material 3  
- Android Gradle Plugin, `libs.versions.toml` orqali versiyalar  
- `android.graphics.RuntimeShader` + AGSL shader matni  

## Talablar

- **minSdk 33** (Android 13+) — shader va minimal API bilan bog‘liq  
- **Android Studio** (loyihada `compileSdk` 36, `targetSdk` 36)  
- JDK 11 (loyihadagi `compileOptions` bo‘yicha)  

## O‘rnatish va ishga tushirish

1. Repozitoriyni klonlang yoki yuklab oling.  
2. Android Studio da loyihani oching.  
3. USB yoki emulyator orqali **API 33+** qurilma tanlang.  
