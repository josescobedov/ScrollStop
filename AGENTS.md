Crea ahora mismo en este directorio los archivos `.agents/skills/android-headless-env/SKILL.md`, `.agents/skills/adb-ui-inspector/SKILL.md` y `AGENTS.md` con las especificaciones que te detallo a continuación, y acto seguido ejecuta todas las fases de principio a fin de forma 100% autónoma sin pedirme confirmación:

1. CONTENIDO PARA `.agents/skills/android-headless-env/SKILL.md`:
- Instalar en PowerShell con winget: `Microsoft.OpenJDK.17`, `Google.PlatformTools`, `Gradle.Gradle` (--silent --accept-package-agreements --accept-source-agreements).
- Si no existe `$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat`, descargar `https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip`, extraer en `$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest`, configurar `JAVA_HOME`, `ANDROID_HOME` y `PATH`, aceptar licencias (`1..15 | % { "y" } | sdkmanager --licenses`) e instalar `"platforms;android-34"` y `"build-tools;34.0.0"`.

2. CONTENIDO PARA `.agents/skills/adb-ui-inspector/SKILL.md`:
- Volcar UI en vivo con `adb shell uiautomator dump /sdcard/window_dump.xml`, `adb pull /sdcard/window_dump.xml ./dump.xml` y extraer `resource-id` únicos con `Select-String`.

3. CONTENIDO PARA `AGENTS.md` Y ARQUITECTURA COMPLETA DE LA APP (`package com.antiscroll.service`, `compileSdk = 34`, `minSdk = 29`):

A) INTERFAZ DE USUARIO LIGERA (`MainActivity.kt` y `activity_main.xml`):
- Usar vistas nativas estándar de Android (sin Jetpack Compose para mantener compilación rápida y APK mínimo) con tema oscuro limpio.
- Declarar `MainActivity` en `AndroidManifest.xml` con `<category android:name="android.intent.category.LAUNCHER" />` para poder abrir la app desde el cajón de aplicaciones.
- Elementos de la pantalla:
  1. Un botón central grande en la parte superior (`btnMasterToggle`) que muestre claramente el estado global ("SERVICIO ACTIVO - PULSAR PARA APAGAR" en verde / "SERVICIO APAGADO - PULSAR PARA ACTIVAR" en rojo). Al pulsarlo, invierte inmediatamente el booleano `master_enabled` en `SharedPreferences("antiscroll_prefs", MODE_PRIVATE)`.
  2. Indicador de estado del permiso de Accesibilidad y un botón secundario para abrir `Settings.ACTION_ACCESSIBILITY_SETTINGS` si el servicio del sistema no está habilitado.
  3. Tarjeta/Sección **Instagram**:
     - `Switch` (`switchIgEnabled`): Activar/desactivar control en Instagram (por defecto `true`).
     - `EditText` numérico (`etIgUsageMinutes`): Minutos de uso permitidos (por defecto `5`).
     - `EditText` numérico (`etIgCooldownMinutes`): Minutos de cooldown (por defecto `15`).
  4. Tarjeta/Sección **TikTok**:
     - `Switch` (`switchTiktokEnabled`): Activar/desactivar control en TikTok (por defecto `true`).
     - `EditText` numérico (`etTiktokUsageMinutes`): Minutos de uso permitidos (por defecto `5`).
     - `EditText` numérico (`etTiktokCooldownMinutes`): Minutos de cooldown (por defecto `15`).
  5. Botón inferior "Guardar Parámetros" (`btnSave`): Guarda todos los valores en `SharedPreferences("antiscroll_prefs", MODE_PRIVATE)`.

B) SERVICIO DE ACCESIBILIDAD (`ScrollBlockerService.kt`) Y CONSUMO CERO:
- Cero Polling: Prohibido usar `Timer`, `Handler` o bucles en segundo plano. Todo se evalúa con `SystemClock.elapsedRealtime()` únicamente cuando entra un evento en `onAccessibilityEvent()`.
- Cero Toasts ni notificaciones al bloquear.
- Sincronización en tiempo real: Leer `SharedPreferences("antiscroll_prefs", MODE_PRIVATE)` en `onServiceConnected()` y registrar un `OnSharedPreferenceChangeListener` para actualizar en memoria al instante `masterEnabled`, `igEnabled`, `igMaxUsageMs`, `igCooldownMs`, `tiktokEnabled`, `tiktokMaxUsageMs` y `tiktokCooldownMs` sin gastar CPU leyendo disco en cada evento.
- Si `masterEnabled == false`, `onAccessibilityEvent()` retorna inmediatamente en la primera línea.
- Filtro XML (`accessibility_service_config.xml`): paquetes `com.instagram.android`, `com.zhiliaoapp.musically`, `com.whatsapp`. Eventos: `typeWindowStateChanged|typeViewScrolled`, `notificationTimeout="150"`, `canRetrieveWindowContent="true"`, `accessibilityFlags="flagReportViewIds"`.

C) MÁQUINA DE ESTADOS (`QuotaTracker`):
- Recibe dinámicamente `maxUsageMs` y `cooldownMs` desde las preferencias de su app correspondiente.
- `SESSION_IDLE_THRESHOLD_MS = 30 * 1000L`.
- En `onSectionEvent(now: Long): Boolean`:
  1. Si `now < cooldownUntil`, devolver `true` (bloquear).
  2. Si `lastEventTime > 0` y `(now - lastEventTime) >= cooldownMs`, resetear `accumulatedUsage = 0L`.
  3. Si `lastEventTime > 0` y `(now - lastEventTime) <= SESSION_IDLE_THRESHOLD_MS`, sumar `accumulatedUsage += (now - lastEventTime)`.
  4. Actualizar `lastEventTime = now`.
  5. Si `accumulatedUsage >= maxUsageMs`, fijar `cooldownUntil = now + cooldownMs`, resetear `accumulatedUsage = 0L` y devolver `true`.
  6. Devolver `false`.

D) REGLAS DE INTERCEPCIÓN:
- **Instagram (`com.instagram.android`):**
  - Si `igEnabled == false`, ignorar y retornar inmediatamente.
  - Feed, Historias y DMs libres. Si detecta nodos con ID que contenga `direct_thread`, `row_thread` o `message_list`, marcar `inInstagramDM = true`.
  - Si detecta el visor de Reels (`clips_viewer_view_pager` o `clips_video_container`):
    * Si viene de DM (`inInstagramDM == true` o `tab_bar` no visible): permitir ver ese Reel siempre (incluso en cooldown), pero en el instante en que ocurra un `TYPE_VIEW_SCROLLED` para pasar al siguiente Reel, ejecutar `performGlobalAction(GLOBAL_ACTION_BACK)` y resetear flags.
    * Si es la pestaña Reels normal: evaluar `instagramQuota.onSectionEvent(now)` y si devuelve `true`, ejecutar `performGlobalAction(GLOBAL_ACTION_BACK)`.
- **TikTok (`com.zhiliaoapp.musically`) y WhatsApp (`com.whatsapp`):**
  - Si llega un evento de `com.whatsapp`, guardar `lastWhatsAppTimestamp = SystemClock.elapsedRealtime()` y retornar en menos de 1ms.
  - En TikTok, si `tiktokEnabled == false`, ignorar y retornar inmediatamente.
  - Si se abre TikTok y `(now - lastWhatsAppTimestamp) < 6000L`, marcar `isExternalTikTokLink = true` y resetear `lastWhatsAppTimestamp = 0L`.
  - Si `isExternalTikTokLink == true`: permitir ver ese vídeo único (incluso en cooldown), pero al primer `TYPE_VIEW_SCROLLED` vertical para pasar al siguiente vídeo, cambiar `isExternalTikTokLink = false` y ejecutar `performGlobalAction(GLOBAL_ACTION_BACK)` seguido de `performGlobalAction(GLOBAL_ACTION_HOME)`.
  - Si `isExternalTikTokLink == false`: evaluar `tiktokQuota.onSectionEvent(now)` y si devuelve `true`, ejecutar `performGlobalAction(GLOBAL_ACTION_HOME)`.

4. COMPILACIÓN, INSTALACIÓN Y ACTIVACIÓN AUTOMÁTICA POR ADB:
- Genera el proyecto completo, compila con `gradle assembleDebug` (corrigiendo autónomamente cualquier error si surge), instala en el móvil conectado con `adb install -r app/build/outputs/apk/debug/app-debug.apk`, activa el servicio en ajustes seguros (`adb shell settings put secure enabled_accessibility_services` preservando los existentes y `accessibility_enabled 1`) y lanza la pantalla principal en el teléfono con `adb shell am start -n com.antiscroll.service/.MainActivity`.