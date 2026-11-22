# Content Tracker (Android, Kotlin + Jetpack Compose)

## Сборка в Android Studio
1. Установите Android Studio (https://developer.android.com/studio). JDK 17 уже встроен.
2. File > Open, выберите папку ContentTracker. Дождитесь Gradle Sync (первый раз нужен интернет).
   Если Studio предложит установить Android SDK Platform 34, согласитесь.
3. Запуск: Device Manager > Create device (эмулятор) или подключите телефон по USB с включённой отладкой, затем кнопка Run.
4. APK: Build > Build Bundle(s) / APK(s) > Build APK(s). Файл: app/build/outputs/apk/debug/app-debug.apk

## Сборка из командной строки
Нужны JDK 17 и Android SDK (переменная ANDROID_HOME). В папке проекта:
    gradle wrapper --gradle-version 8.9
    ./gradlew assembleDebug        (Windows: gradlew.bat assembleDebug)

## Значок и название
Значок задан векторно (res/drawable/ic_launcher_foreground.xml, цвет фона в res/values/ic_launcher_background.xml).
Название: res/values/strings.xml, ключ app_name. Дата релиза на экране «О программе»: константа RELEASE в Ui.kt,
версия берётся из versionName в app/build.gradle.kts.
