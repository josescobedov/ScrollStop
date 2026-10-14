# 🛑 ScrollStop

> **Control de scroll compulsivo inteligente para Instagram y TikTok.**  
> Aplicación nativa y ligera para Android que intercepta el scroll infinito mediante un **Servicio de Accesibilidad de consumo cero**.

---

## ✨ Características Principales

- **Fondo OLED Puro (`#000000`) & Diseño Neón**: Interfaz moderna, limpia y optimizada para pantallas AMOLED con acentos en verde esmeralda neón (`#00F5A0`) y cian neón (`#00E5FF`).
- **Control Inteligente de Reels Compartidos**:
  - Permite ver los Reels o vídeos enviados por amigos a través de **Mensajes Directos (DM)** o enlaces compartidos (**WhatsApp, Telegram, etc.**).
  - Al primer intento de deslizar hacia abajo para entrar en el bucle infinito, el servicio **te expulsa automáticamente**.
- **Gestión de Cuotas y Cooldown**:
  - Configuración independiente para Instagram y TikTok.
  - Temporizador de uso y tiempo de descanso (*cooldown*).
  - Si se establece en `0 minutos`, se bloquea el feed de Reels de inmediato, permitiendo únicamente los enlaces recibidos.
- **Perfiles de Protección**:
  - 🎓 **Estudio**: Restricciones 24/7 (bloqueo continuo).
  - 🌙 **Diario (22h - 11h)**: Restricciones activas durante la noche y primeras horas de la mañana; libre el resto del día.
  - 🕊️ **Todo (Libre)**: Modo sin restricciones temporales.
- **Interruptor Maestro (Master Toggle)**: Pausa o reactiva todo el sistema con un solo toque desde la pantalla principal.
- **Control Preciso (+ / -)**: Botones hápticos integrados para ajustar cómodamente los minutos de uso y descanso.

---

## 🔒 Privacidad y Seguridad

- **Cero Conexión a Red**: La aplicación **no declara el permiso de Internet (`android.permission.INTERNET`)**. Es técnicamente imposible que transmita datos fuera del dispositivo.
- **Sin recopilación de datos**: No utiliza bases de datos de seguimiento, analíticas ni SDKs de terceros.
- **Consumo Cero**: Sin procesos en segundo plano (*polling* ni temporizadores activos); solo se activa reactivamente ante eventos del sistema cuando se usan las apps objetivo.

---

## 🛠️ Tecnologías y Requisitos

- **Plataforma:** Android (minSdk 29, targetSdk 34)
- **Lenguaje:** Kotlin
- **Arquitectura:** Single-Activity nativa con `AccessibilityService` optimizado
- **Compilador:** Gradle 8.5 / JDK 17

---

## 🚀 Compilación e Instalación

### Compilación con Gradle
```bash
gradle assembleDebug
```

El archivo APK generado estará disponible en:
```text
app/build/outputs/apk/debug/ScrollStop-v1.0.apk
```

### Instalación vía ADB
```bash
adb install -r app/build/outputs/apk/debug/ScrollStop-v1.0.apk
```

---

## 📄 Licencia

Proyecto personal de código abierto desarrollado para bienestar digital.
