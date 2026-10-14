package com.antiscroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScrollBlockerService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val TAG = "ScrollBlockerService"

        private const val PREFS_NAME = "antiscroll_prefs"
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_IG_ENABLED = "ig_enabled"
        private const val KEY_IG_ALLOW_SHARED = "ig_allow_shared"
        private const val KEY_IG_USAGE_MINUTES = "ig_usage_minutes"
        private const val KEY_IG_COOLDOWN_MINUTES = "ig_cooldown_minutes"
        private const val KEY_TIKTOK_ENABLED = "tiktok_enabled"
        private const val KEY_TIKTOK_ALLOW_SHARED = "tiktok_allow_shared"
        private const val KEY_TIKTOK_USAGE_MINUTES = "tiktok_usage_minutes"
        private const val KEY_TIKTOK_COOLDOWN_MINUTES = "tiktok_cooldown_minutes"
        private const val KEY_ACTIVE_PROFILE = "active_profile"

        const val PROFILE_STUDY = "study"
        const val PROFILE_DAILY = "daily"
        const val PROFILE_ALL = "all"

        private const val PACKAGE_INSTAGRAM = "com.instagram.android"
        private const val PACKAGE_TIKTOK = "com.zhiliaoapp.musically"
        private const val PACKAGE_WHATSAPP = "com.whatsapp"
    }

    private var prefs: SharedPreferences? = null

    // Cache en memoria sincronizada en tiempo real sin polling
    @Volatile private var masterEnabled: Boolean = true
    @Volatile private var activeProfile: String = PROFILE_STUDY
    @Volatile private var igEnabled: Boolean = true
    @Volatile private var igAllowShared: Boolean = true
    @Volatile private var igMaxUsageMs: Long = 5 * 60 * 1000L
    @Volatile private var igCooldownMs: Long = 15 * 60 * 1000L

    @Volatile private var tiktokEnabled: Boolean = true
    @Volatile private var tiktokAllowShared: Boolean = true
    @Volatile private var tiktokMaxUsageMs: Long = 5 * 60 * 1000L
    @Volatile private var tiktokCooldownMs: Long = 15 * 60 * 1000L

    private lateinit var instagramQuota: QuotaTracker
    private lateinit var tiktokQuota: QuotaTracker

    // Contexto de origen (DMs o enlaces externos)
    private var inInstagramDM: Boolean = false
    private var lastExternalTimestamp: Long = 0L

    // Estados de visualización
    private var isViewingSharedReel: Boolean = false
    private var sharedReelOpenTime: Long = 0L
    private var hasHadSenderHeader: Boolean = false
    private var isViewingSharedTikTok: Boolean = false
    private var sharedTiktokOpenTime: Long = 0L
    private var lastReelBlockTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.registerOnSharedPreferenceChangeListener(this)
        loadPreferences()

        instagramQuota = QuotaTracker(igMaxUsageMs, igCooldownMs)
        tiktokQuota = QuotaTracker(tiktokMaxUsageMs, tiktokCooldownMs)
        Log.i(TAG, "ScrollBlockerService conectado y listo.")
    }

    private fun loadPreferences() {
        prefs?.let { p ->
            masterEnabled = p.getBoolean(KEY_MASTER_ENABLED, true)
            activeProfile = p.getString(KEY_ACTIVE_PROFILE, PROFILE_STUDY) ?: PROFILE_STUDY
            igEnabled = p.getBoolean(KEY_IG_ENABLED, true)
            igAllowShared = p.getBoolean(KEY_IG_ALLOW_SHARED, true)
            val igUsageMin = p.getInt(KEY_IG_USAGE_MINUTES, 5)
            val igCooldownMin = p.getInt(KEY_IG_COOLDOWN_MINUTES, 15)
            igMaxUsageMs = igUsageMin * 60 * 1000L
            igCooldownMs = igCooldownMin * 60 * 1000L

            tiktokEnabled = p.getBoolean(KEY_TIKTOK_ENABLED, true)
            tiktokAllowShared = p.getBoolean(KEY_TIKTOK_ALLOW_SHARED, true)
            val tiktokUsageMin = p.getInt(KEY_TIKTOK_USAGE_MINUTES, 5)
            val tiktokCooldownMin = p.getInt(KEY_TIKTOK_COOLDOWN_MINUTES, 15)
            tiktokMaxUsageMs = tiktokUsageMin * 60 * 1000L
            tiktokCooldownMs = tiktokCooldownMin * 60 * 1000L

            if (::instagramQuota.isInitialized) {
                instagramQuota.maxUsageMs = igMaxUsageMs
                instagramQuota.cooldownMs = igCooldownMs
            }
            if (::tiktokQuota.isInitialized) {
                tiktokQuota.maxUsageMs = tiktokMaxUsageMs
                tiktokQuota.cooldownMs = tiktokCooldownMs
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        loadPreferences()
    }

    private fun isDailyScheduleActive(): Boolean {
        val cal = java.util.Calendar.getInstance()
        val currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val startMinutes = 22 * 60 // 22:00 = 1320
        val endMinutes = 11 * 60   // 11:00 = 660

        // Horario nocturno que cruza la medianoche: activo desde las 22:00 hasta las 11:00
        return currentMinutes >= startMinutes || currentMinutes < endMinutes
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!masterEnabled || event == null) return

        // 1. Evaluación del perfil activo
        // Perfil "all" (Todo/Libre): No aplica restricciones
        if (activeProfile == PROFILE_ALL) return

        // Perfil "daily" (Diario): Restricciones activas únicamente de 22:00 a 11:00
        if (activeProfile == PROFILE_DAILY && !isDailyScheduleActive()) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        val now = SystemClock.elapsedRealtime()

        // 1. WhatsApp, Telegram u otras apps externas desde donde envían enlaces
        if (packageName == PACKAGE_WHATSAPP ||
            packageName.contains("messaging") ||
            packageName.contains("browser") ||
            packageName.contains("chrome") ||
            packageName.contains("telegram")) {
            lastExternalTimestamp = now
            return
        }

        when (packageName) {
            PACKAGE_INSTAGRAM -> handleInstagramEvent(event, now)
            PACKAGE_TIKTOK -> handleTikTokEvent(event, now)
        }
    }

    private fun handleInstagramEvent(event: AccessibilityEvent, now: Long) {
        if (!igEnabled) return

        val rootNode = rootInActiveWindow ?: return
        try {
            // 1. Detectar si estamos activamente en el chat de Mensajes Directos (DMs)
            val hasDirectNodes = hasMatchingNode(rootNode) { id, desc ->
                // NOTA CRÍTICA: com.instagram.android:id/direct_share_button existe en TODOS los Reels y posts.
                // Debe excluirse expresamente para no confundir un Reel con un chat de DM.
                if (id.contains("direct_share") || id.contains("direct_send") || id.contains("share_button")) {
                    return@hasMatchingNode false
                }

                id.contains("direct_thread") ||
                id.contains("row_thread") ||
                id.contains("message_list") ||
                id.contains("thread_fragment") ||
                id.contains("thread_messages") ||
                id.contains("inbox_") ||
                id.contains("message_composer") ||
                id.contains("message_content_portrait_xma") ||
                (id.contains("direct_") && !id.contains("share")) ||
                desc.contains("Direct", ignoreCase = true) ||
                desc.contains("Mensaje", ignoreCase = true) ||
                desc.contains("Chat", ignoreCase = true)
            }

            if (hasDirectNodes) {
                inInstagramDM = true
                isViewingSharedReel = false
                hasHadSenderHeader = false
            }

            // 2. Comprobar la barra inferior de navegación (tab_bar)
            val hasTabBar = hasMatchingNode(rootNode) { id, _ ->
                id == "com.instagram.android:id/tab_bar"
            }

            // Comprobar si la pestaña de Reels (clips_tab) está activamente seleccionada
            val isClipsTabSelected = isClipsTabActive(rootNode)

            // Comprobar si estamos en Home/Feed normal
            val isFeedTabSelected = isFeedTabActive(rootNode)

            // Si estamos en la pestaña Feed principal (Home), permitir libre navegación (Feed, Historias, Posts)
            if (isFeedTabSelected) {
                isViewingSharedReel = false
                inInstagramDM = false
                hasHadSenderHeader = false
                return
            }

            // 3. Si el usuario hace click o navega a la pestaña REELS (scroll infinito en app)
            if (isClipsTabSelected) {
                isViewingSharedReel = false
                inInstagramDM = false
                hasHadSenderHeader = false

                // Evaluar si la cuota de Reels de feed ha expirado o está en cooldown
                val shouldBlock = instagramQuota.onSectionEvent(now)
                if (shouldBlock) {
                    if ((now - lastReelBlockTime) > 800L) {
                        lastReelBlockTime = now
                        Log.i(TAG, "Reels de feed bloqueado por cuota o cooldown. Expulsando hacia atrás.")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }
                return
            }

            // 4. Detectar si estamos en el visor de Reels (Clips)
            // IMPORTANTE:
            // - No debe tener la barra inferior de pestañas (hasTabBar == false)
            // - No debe estar dentro de la lista de mensajes de DM (hasDirectNodes == false)
            // - Debe contener contenedor de clips (clips_viewer o clips_video_container)
            // NOTA: NO incluir reel_viewer porque en Instagram reel_viewer corresponde a HISTORIAS (Stories).
            val isReelsViewer = !hasTabBar && !hasDirectNodes && hasMatchingNode(rootNode) { id, _ ->
                id.contains("clips_viewer") ||
                id.contains("clips_video_container")
            }

            val cameFromExternal = (now - lastExternalTimestamp) < 15000L

            if (isReelsViewer) {
                // Comprobar si está presente la cabecera del remitente (ej. "irene cudeiro")
                val hasSenderHeader = hasMatchingNode(rootNode) { id, _ ->
                    id.contains("sender_username") || id.contains("sender_profile_pic")
                }
                val isSharedReel = inInstagramDM || cameFromExternal || hasSenderHeader

                if (!isViewingSharedReel) {
                    // Acabamos de entrar en el visor de Reels
                    if (isSharedReel || igAllowShared) {
                        // Reel individual compartido: PERMITIDO SIEMPRE (incluso con cuota 0 o en cooldown)
                        isViewingSharedReel = true
                        sharedReelOpenTime = now
                        hasHadSenderHeader = hasSenderHeader
                        Log.i(TAG, "Reel individual compartido abierto (compartido=$isSharedReel). Permitido sin importar cuota/cooldown.")
                    } else {
                        // Reel abierto desde el feed u otro origen no compartido:
                        val shouldBlock = instagramQuota.onSectionEvent(now)
                        if (shouldBlock) {
                            if ((now - lastReelBlockTime) > 800L) {
                                lastReelBlockTime = now
                                Log.i(TAG, "Reel de feed bloqueado por cuota/cooldown.")
                                performGlobalAction(GLOBAL_ACTION_BACK)
                            }
                            return
                        }
                    }
                }

                // Si está viendo el Reel compartido permitido:
                if (isViewingSharedReel) {
                    val timeSinceOpen = now - sharedReelOpenTime

                    if (hasSenderHeader) {
                        hasHadSenderHeader = true
                    } else if (hasHadSenderHeader && timeSinceOpen >= 800L) {
                        // Si el Reel tenía cabecera de remitente y ahora ha desaparecido,
                        // significa que el usuario ha avanzado al siguiente Reel en la cola.
                        if ((now - lastReelBlockTime) > 800L) {
                            lastReelBlockTime = now
                            Log.i(TAG, "Detectado avance al siguiente Reel (cabecera remitente desapareció). Expulsando con BACK.")
                            isViewingSharedReel = false
                            inInstagramDM = false
                            hasHadSenderHeader = false
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                        return
                    }

                    // Detección directa por gesto de scroll
                    if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                        // Durante los primeros 800ms tras abrir el Reel, ignorar scroll técnico de inflado
                        if (timeSinceOpen < 800L) {
                            Log.d(TAG, "Ignorando scroll de layout inicial del Reel (${timeSinceOpen}ms).")
                            return
                        }

                        // Comprobar si el scroll proviene de la lista de comentarios
                        val source = event.source
                        var isCommentScroll = false
                        if (source != null) {
                            try {
                                val sId = source.viewIdResourceName?.lowercase() ?: ""
                                if (sId.contains("comment") || sId.contains("bottom_sheet")) {
                                    isCommentScroll = true
                                }
                            } finally {
                                source.recycle()
                            }
                        }

                        if (isCommentScroll) {
                            return
                        }

                        // Scroll para pasar al siguiente Reel tras visualización
                        if ((now - lastReelBlockTime) > 800L) {
                            lastReelBlockTime = now
                            Log.i(TAG, "Scroll detectado en Reel compartido tras ${timeSinceOpen}ms. Expulsando con BACK.")
                            isViewingSharedReel = false
                            inInstagramDM = false
                            hasHadSenderHeader = false
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                        return
                    }
                } else {
                    // Está en el visor de Reels normal (navegando feed/reels permitidos por tiempo)
                    val shouldBlock = instagramQuota.onSectionEvent(now)
                    if (shouldBlock) {
                        if ((now - lastReelBlockTime) > 800L) {
                            lastReelBlockTime = now
                            Log.i(TAG, "Tiempo de uso de Reels agotado. Expulsando hacia atrás al feed.")
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                        return
                    }
                }
            } else {
                // Fuera del visor de Reels
                if (hasTabBar || hasDirectNodes) {
                    isViewingSharedReel = false
                    hasHadSenderHeader = false
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun handleTikTokEvent(event: AccessibilityEvent, now: Long) {
        if (!tiktokEnabled) return

        val cameFromExternal = (now - lastExternalTimestamp) < 15000L
        if (cameFromExternal) {
            lastExternalTimestamp = 0L
            if (tiktokAllowShared) {
                isViewingSharedTikTok = true
                sharedTiktokOpenTime = now
                Log.i(TAG, "TikTok compartido abierto permitido.")
            }
        }

        if (isViewingSharedTikTok) {
            // Si está viendo el vídeo único compartido de TikTok:
            // Al primer intento de scroll vertical para pasar al siguiente vídeo tras la carga inicial, expulsar
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                val timeSinceOpen = now - sharedTiktokOpenTime
                if (timeSinceOpen < 1500L) {
                    Log.d(TAG, "Ignorando scroll de montaje inicial en TikTok (${timeSinceOpen}ms).")
                    return
                }

                Log.i(TAG, "Scroll detectado en TikTok compartido tras ${timeSinceOpen}ms. Expulsando a Home.")
                isViewingSharedTikTok = false
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }
        } else {
            // Feed normal de TikTok
            if (tiktokQuota.onSectionEvent(now)) {
                Log.i(TAG, "TikTok bloqueado por cuota. Mandando a Home.")
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun isClipsTabActive(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val viewId = node.viewIdResourceName
        if (viewId != null && viewId == "com.instagram.android:id/clips_tab" && node.isSelected) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = isClipsTabActive(child)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun isFeedTabActive(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val viewId = node.viewIdResourceName
        if (viewId != null && viewId == "com.instagram.android:id/feed_tab" && node.isSelected) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = isFeedTabActive(child)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun hasMatchingNode(
        node: AccessibilityNodeInfo?,
        predicate: (id: String, desc: String) -> Boolean
    ): Boolean {
        if (node == null) return false
        val id = node.viewIdResourceName?.lowercase() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        if (predicate(id, desc)) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = hasMatchingNode(child, predicate)
            child.recycle()
            if (found) return true
        }
        return false
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        prefs?.unregisterOnSharedPreferenceChangeListener(this)
    }
}
