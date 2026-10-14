package com.antiscroll.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "antiscroll_prefs"
        const val KEY_MASTER_ENABLED = "master_enabled"
        const val KEY_ACTIVE_PROFILE = "active_profile"
        const val KEY_IG_ENABLED = "ig_enabled"
        const val KEY_IG_ALLOW_SHARED = "ig_allow_shared"
        const val KEY_IG_USAGE_MINUTES = "ig_usage_minutes"
        const val KEY_IG_COOLDOWN_MINUTES = "ig_cooldown_minutes"
        const val KEY_TIKTOK_ENABLED = "tiktok_enabled"
        const val KEY_TIKTOK_ALLOW_SHARED = "tiktok_allow_shared"
        const val KEY_TIKTOK_USAGE_MINUTES = "tiktok_usage_minutes"
        const val KEY_TIKTOK_COOLDOWN_MINUTES = "tiktok_cooldown_minutes"

        const val PROFILE_STUDY = "study"
        const val PROFILE_DAILY = "daily"
        const val PROFILE_ALL = "all"
    }

    private lateinit var prefs: SharedPreferences

    // Master Card
    private lateinit var cardMasterToggle: FrameLayout
    private lateinit var viewMasterDot: View
    private lateinit var tvMasterStatusBadge: TextView
    private lateinit var tvMasterTitle: TextView
    private lateinit var tvMasterSubtitle: TextView
    private lateinit var btnMasterToggle: Button

    // Profile Selector
    private lateinit var btnProfileStudy: TextView
    private lateinit var btnProfileDaily: TextView
    private lateinit var btnProfileAll: TextView
    private lateinit var tvProfileDescription: TextView
    private var selectedProfile: String = PROFILE_STUDY

    // Accessibility Status
    private lateinit var viewA11yDot: View
    private lateinit var tvA11yStatus: TextView
    private lateinit var btnOpenA11ySettings: TextView

    // Instagram Card
    private lateinit var cardInstagram: View
    private lateinit var switchIgEnabled: Switch
    private lateinit var switchIgAllowShared: Switch
    private lateinit var btnIgUsageMinus: TextView
    private lateinit var etIgUsageMinutes: EditText
    private lateinit var btnIgUsagePlus: TextView
    private lateinit var btnIgCooldownMinus: TextView
    private lateinit var etIgCooldownMinutes: EditText
    private lateinit var btnIgCooldownPlus: TextView

    // TikTok Card
    private lateinit var cardTiktok: View
    private lateinit var switchTiktokEnabled: Switch
    private lateinit var switchTiktokAllowShared: Switch
    private lateinit var btnTiktokUsageMinus: TextView
    private lateinit var etTiktokUsageMinutes: EditText
    private lateinit var btnTiktokUsagePlus: TextView
    private lateinit var btnTiktokCooldownMinus: TextView
    private lateinit var etTiktokCooldownMinutes: EditText
    private lateinit var btnTiktokCooldownPlus: TextView

    // Save Button
    private lateinit var btnSave: Button

    private var pulseAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        initViews()
        loadValues()
        setupListeners()
        startNeonPulseAnimation()
    }

    override fun onResume() {
        super.onResume()
        updateA11yStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnimator?.cancel()
    }

    private fun initViews() {
        cardMasterToggle = findViewById(R.id.cardMasterToggle)
        viewMasterDot = findViewById(R.id.viewMasterDot)
        tvMasterStatusBadge = findViewById(R.id.tvMasterStatusBadge)
        tvMasterTitle = findViewById(R.id.tvMasterTitle)
        tvMasterSubtitle = findViewById(R.id.tvMasterSubtitle)
        btnMasterToggle = findViewById(R.id.btnMasterToggle)

        btnProfileStudy = findViewById(R.id.btnProfileStudy)
        btnProfileDaily = findViewById(R.id.btnProfileDaily)
        btnProfileAll = findViewById(R.id.btnProfileAll)
        tvProfileDescription = findViewById(R.id.tvProfileDescription)

        viewA11yDot = findViewById(R.id.viewA11yDot)
        tvA11yStatus = findViewById(R.id.tvA11yStatus)
        btnOpenA11ySettings = findViewById(R.id.btnOpenA11ySettings)

        cardInstagram = findViewById(R.id.cardInstagram)
        switchIgEnabled = findViewById(R.id.switchIgEnabled)
        switchIgAllowShared = findViewById(R.id.switchIgAllowShared)
        btnIgUsageMinus = findViewById(R.id.btnIgUsageMinus)
        etIgUsageMinutes = findViewById(R.id.etIgUsageMinutes)
        btnIgUsagePlus = findViewById(R.id.btnIgUsagePlus)
        btnIgCooldownMinus = findViewById(R.id.btnIgCooldownMinus)
        etIgCooldownMinutes = findViewById(R.id.etIgCooldownMinutes)
        btnIgCooldownPlus = findViewById(R.id.btnIgCooldownPlus)

        cardTiktok = findViewById(R.id.cardTiktok)
        switchTiktokEnabled = findViewById(R.id.switchTiktokEnabled)
        switchTiktokAllowShared = findViewById(R.id.switchTiktokAllowShared)
        btnTiktokUsageMinus = findViewById(R.id.btnTiktokUsageMinus)
        etTiktokUsageMinutes = findViewById(R.id.etTiktokUsageMinutes)
        btnTiktokUsagePlus = findViewById(R.id.btnTiktokUsagePlus)
        btnTiktokCooldownMinus = findViewById(R.id.btnTiktokCooldownMinus)
        etTiktokCooldownMinutes = findViewById(R.id.etTiktokCooldownMinutes)
        btnTiktokCooldownPlus = findViewById(R.id.btnTiktokCooldownPlus)

        btnSave = findViewById(R.id.btnSave)
    }

    private fun loadValues() {
        selectedProfile = prefs.getString(KEY_ACTIVE_PROFILE, PROFILE_STUDY) ?: PROFILE_STUDY
        updateProfileUi(selectedProfile, animate = false)

        val masterEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        updateMasterToggleUi(masterEnabled, animate = false)

        val igEnabled = prefs.getBoolean(KEY_IG_ENABLED, true)
        switchIgEnabled.isChecked = igEnabled
        updateCardAlpha(cardInstagram, igEnabled)
        switchIgAllowShared.isChecked = prefs.getBoolean(KEY_IG_ALLOW_SHARED, true)
        etIgUsageMinutes.setText(prefs.getInt(KEY_IG_USAGE_MINUTES, 5).toString())
        etIgCooldownMinutes.setText(prefs.getInt(KEY_IG_COOLDOWN_MINUTES, 15).toString())

        val tiktokEnabled = prefs.getBoolean(KEY_TIKTOK_ENABLED, true)
        switchTiktokEnabled.isChecked = tiktokEnabled
        updateCardAlpha(cardTiktok, tiktokEnabled)
        switchTiktokAllowShared.isChecked = prefs.getBoolean(KEY_TIKTOK_ALLOW_SHARED, true)
        etTiktokUsageMinutes.setText(prefs.getInt(KEY_TIKTOK_USAGE_MINUTES, 5).toString())
        etTiktokCooldownMinutes.setText(prefs.getInt(KEY_TIKTOK_COOLDOWN_MINUTES, 15).toString())
    }

    private fun updateProfileUi(profile: String, animate: Boolean) {
        selectedProfile = profile

        val unselectedBg = R.drawable.bg_profile_unselected
        val selectedBg = R.drawable.bg_profile_selected
        val unselectedColor = ContextCompat.getColor(this, R.color.text_secondary)
        val selectedColor = ContextCompat.getColor(this, R.color.neon_emerald)

        btnProfileStudy.setBackgroundResource(if (profile == PROFILE_STUDY) selectedBg else unselectedBg)
        btnProfileStudy.setTextColor(if (profile == PROFILE_STUDY) selectedColor else unselectedColor)

        btnProfileDaily.setBackgroundResource(if (profile == PROFILE_DAILY) selectedBg else unselectedBg)
        btnProfileDaily.setTextColor(if (profile == PROFILE_DAILY) selectedColor else unselectedColor)

        btnProfileAll.setBackgroundResource(if (profile == PROFILE_ALL) selectedBg else unselectedBg)
        btnProfileAll.setTextColor(if (profile == PROFILE_ALL) selectedColor else unselectedColor)

        when (profile) {
            PROFILE_STUDY -> {
                tvProfileDescription.text = "Restricciones activas siempre (24/7). Bloqueo constante de Reels y TikTok."
            }
            PROFILE_DAILY -> {
                tvProfileDescription.text = "Restricciones activas de 22:00 a 11:00 para proteger tus noches y mañanas. Libre de 11:00 a 22:00."
            }
            PROFILE_ALL -> {
                tvProfileDescription.text = "Modo Libre sin restricciones. Todo el contenido y scroll están permitidos."
            }
        }

        val masterEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        updateMasterToggleUi(masterEnabled, animate = false)

        if (animate) {
            animateSpringBounce(tvProfileDescription)
        }
    }

    private fun updateMasterToggleUi(enabled: Boolean, animate: Boolean) {
        if (!enabled) {
            cardMasterToggle.setBackgroundResource(R.drawable.bg_card_master_inactive)
            viewMasterDot.setBackgroundResource(R.drawable.bg_pill_a11y_inactive)
            tvMasterStatusBadge.text = "ESCUDO EN PAUSA"
            tvMasterStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_rose))
            tvMasterTitle.text = "Protección desactivada manualmente"
            tvMasterSubtitle.text = "Los algoritmos infinitos están libres. Toca para reactivar el escudo."
        } else {
            when (selectedProfile) {
                PROFILE_ALL -> {
                    cardMasterToggle.setBackgroundResource(R.drawable.bg_card_master_inactive)
                    viewMasterDot.setBackgroundResource(R.drawable.bg_pill_a11y_inactive)
                    tvMasterStatusBadge.text = "PERFIL TODO (LIBRE)"
                    tvMasterStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan))
                    tvMasterTitle.text = "Sin restricciones aplicadas"
                    tvMasterSubtitle.text = "El perfil actual permite todo el contenido sin límites."
                }
                PROFILE_DAILY -> {
                    cardMasterToggle.setBackgroundResource(R.drawable.bg_card_master_active)
                    viewMasterDot.setBackgroundResource(R.drawable.bg_pill_a11y_active)
                    tvMasterStatusBadge.text = "PERFIL DIARIO (22h - 11h)"
                    tvMasterStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_emerald))
                    tvMasterTitle.text = "Protección programada activa"
                    tvMasterSubtitle.text = "Restricciones vigentes de 22:00 a 11:00. Toca para pausar."
                }
                else -> {
                    cardMasterToggle.setBackgroundResource(R.drawable.bg_card_master_active)
                    viewMasterDot.setBackgroundResource(R.drawable.bg_pill_a11y_active)
                    tvMasterStatusBadge.text = "PERFIL ESTUDIO (24/7)"
                    tvMasterStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_emerald))
                    tvMasterTitle.text = "Protección Anti-Scroll activada"
                    tvMasterSubtitle.text = "Reels y vídeos infinitos bloqueados siempre. Toca para pausar."
                }
            }
        }

        if (animate) {
            animateSpringBounce(cardMasterToggle)
        }
    }

    private fun setupListeners() {
        cardMasterToggle.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val currentState = prefs.getBoolean(KEY_MASTER_ENABLED, true)
            val newState = !currentState
            prefs.edit().putBoolean(KEY_MASTER_ENABLED, newState).apply()
            updateMasterToggleUi(newState, animate = true)
        }

        btnMasterToggle.setOnClickListener {
            cardMasterToggle.performClick()
        }

        // Profile Clicks
        btnProfileStudy.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateSpringBounce(it)
            updateProfileUi(PROFILE_STUDY, animate = true)
            prefs.edit().putString(KEY_ACTIVE_PROFILE, PROFILE_STUDY).apply()
        }

        btnProfileDaily.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateSpringBounce(it)
            updateProfileUi(PROFILE_DAILY, animate = true)
            prefs.edit().putString(KEY_ACTIVE_PROFILE, PROFILE_DAILY).apply()
        }

        btnProfileAll.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateSpringBounce(it)
            updateProfileUi(PROFILE_ALL, animate = true)
            prefs.edit().putString(KEY_ACTIVE_PROFILE, PROFILE_ALL).apply()
        }

        switchIgEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateSpringBounce(buttonView)
            updateCardAlpha(cardInstagram, isChecked)
        }

        switchTiktokEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateSpringBounce(buttonView)
            updateCardAlpha(cardTiktok, isChecked)
        }

        switchIgAllowShared.setOnCheckedChangeListener { buttonView, _ ->
            buttonView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateSpringBounce(buttonView)
        }

        switchTiktokAllowShared.setOnCheckedChangeListener { buttonView, _ ->
            buttonView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateSpringBounce(buttonView)
        }

        btnOpenA11ySettings.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            animateSpringBounce(it)
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        setupStepper(btnIgUsageMinus, btnIgUsagePlus, etIgUsageMinutes, minVal = 0, step = 1)
        setupStepper(btnIgCooldownMinus, btnIgCooldownPlus, etIgCooldownMinutes, minVal = 0, step = 1)
        setupStepper(btnTiktokUsageMinus, btnTiktokUsagePlus, etTiktokUsageMinutes, minVal = 0, step = 1)
        setupStepper(btnTiktokCooldownMinus, btnTiktokCooldownPlus, etTiktokCooldownMinutes, minVal = 0, step = 1)

        btnSave.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            animateSpringBounce(it)

            val igUsage = etIgUsageMinutes.text.toString().toIntOrNull() ?: 5
            val igCooldown = etIgCooldownMinutes.text.toString().toIntOrNull() ?: 15
            val tiktokUsage = etTiktokUsageMinutes.text.toString().toIntOrNull() ?: 5
            val tiktokCooldown = etTiktokCooldownMinutes.text.toString().toIntOrNull() ?: 15

            prefs.edit()
                .putString(KEY_ACTIVE_PROFILE, selectedProfile)
                .putBoolean(KEY_IG_ENABLED, switchIgEnabled.isChecked)
                .putBoolean(KEY_IG_ALLOW_SHARED, switchIgAllowShared.isChecked)
                .putInt(KEY_IG_USAGE_MINUTES, igUsage)
                .putInt(KEY_IG_COOLDOWN_MINUTES, igCooldown)
                .putBoolean(KEY_TIKTOK_ENABLED, switchTiktokEnabled.isChecked)
                .putBoolean(KEY_TIKTOK_ALLOW_SHARED, switchTiktokAllowShared.isChecked)
                .putInt(KEY_TIKTOK_USAGE_MINUTES, tiktokUsage)
                .putInt(KEY_TIKTOK_COOLDOWN_MINUTES, tiktokCooldown)
                .apply()

            val originalText = "Aplicar Configuración"
            btnSave.text = "✓ ¡Configuración Guardada!"
            Handler(Looper.getMainLooper()).postDelayed({
                btnSave.text = originalText
            }, 2000L)

            Toast.makeText(this, "Parámetros aplicados correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupStepper(btnMinus: View, btnPlus: View, editText: EditText, minVal: Int = 0, step: Int = 1) {
        btnMinus.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateSpringBounce(it)
            val current = editText.text.toString().toIntOrNull() ?: minVal
            val updated = (current - step).coerceAtLeast(minVal)
            editText.setText(updated.toString())
        }

        btnPlus.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateSpringBounce(it)
            val current = editText.text.toString().toIntOrNull() ?: minVal
            val updated = current + step
            editText.setText(updated.toString())
        }
    }

    private fun updateCardAlpha(view: View, enabled: Boolean) {
        val targetAlpha = if (enabled) 1.0f else 0.55f
        view.animate()
            .alpha(targetAlpha)
            .setDuration(250L)
            .start()
    }

    private fun animateSpringBounce(view: View) {
        val scaleXDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.94f)
        val scaleYDown = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.94f)
        scaleXDown.duration = 90
        scaleYDown.duration = 90

        val scaleXUp = ObjectAnimator.ofFloat(view, "scaleX", 0.94f, 1f)
        val scaleYUp = ObjectAnimator.ofFloat(view, "scaleY", 0.94f, 1f)
        scaleXUp.duration = 200
        scaleYUp.duration = 200
        scaleXUp.interpolator = OvershootInterpolator(3.0f)
        scaleYUp.interpolator = OvershootInterpolator(3.0f)

        val downSet = AnimatorSet().apply { playTogether(scaleXDown, scaleYDown) }
        val upSet = AnimatorSet().apply { playTogether(scaleXUp, scaleYUp) }

        AnimatorSet().apply {
            playSequentially(downSet, upSet)
            start()
        }
    }

    private fun startNeonPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofFloat(viewMasterDot, "alpha", 1f, 0.35f, 1f).apply {
            duration = 1800L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun updateA11yStatus() {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val isServiceRunning = enabledServices?.any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
                    it.resolveInfo.serviceInfo.name == ScrollBlockerService::class.java.name
        } ?: false

        if (isServiceRunning) {
            tvA11yStatus.text = "Servicio de accesibilidad: ACTIVO"
            tvA11yStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_emerald))
            viewA11yDot.setBackgroundResource(R.drawable.bg_pill_a11y_active)
        } else {
            tvA11yStatus.text = "Servicio de accesibilidad: DESACTIVADO"
            tvA11yStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_rose))
            viewA11yDot.setBackgroundResource(R.drawable.bg_pill_a11y_inactive)
        }
    }
}
