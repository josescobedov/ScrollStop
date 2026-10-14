package com.antiscroll.service

class QuotaTracker(
    @Volatile var maxUsageMs: Long,
    @Volatile var cooldownMs: Long
) {
    companion object {
        const val SESSION_IDLE_THRESHOLD_MS = 30 * 1000L
    }

    private var lastEventTime: Long = 0L
    private var accumulatedUsage: Long = 0L
    private var cooldownUntil: Long = 0L

    @Synchronized
    fun onSectionEvent(now: Long): Boolean {
        // Si el tiempo permitido es 0 (o menor), bloquear inmediatamente siempre
        if (maxUsageMs <= 0L) {
            return true
        }

        // 1. Si now < cooldownUntil, devolver true (bloquear por cooldown activo)
        if (now < cooldownUntil) {
            return true
        }

        // 2. Si lastEventTime > 0 y (now - lastEventTime) >= cooldownMs, resetear accumulatedUsage = 0L
        if (lastEventTime > 0L && (now - lastEventTime) >= cooldownMs) {
            accumulatedUsage = 0L
        }

        // 3. Si lastEventTime > 0 y (now - lastEventTime) <= SESSION_IDLE_THRESHOLD_MS, sumar accumulatedUsage += (now - lastEventTime)
        if (lastEventTime > 0L && (now - lastEventTime) <= SESSION_IDLE_THRESHOLD_MS) {
            accumulatedUsage += (now - lastEventTime)
        }

        // 4. Actualizar lastEventTime = now
        lastEventTime = now

        // 5. Si accumulatedUsage >= maxUsageMs, fijar cooldownUntil = now + cooldownMs, resetear accumulatedUsage = 0L y devolver true
        if (accumulatedUsage >= maxUsageMs) {
            cooldownUntil = now + cooldownMs
            accumulatedUsage = 0L
            return true
        }

        // 6. Devolver false (permitido)
        return false
    }

    @Synchronized
    fun reset() {
        lastEventTime = 0L
        accumulatedUsage = 0L
        cooldownUntil = 0L
    }
}
