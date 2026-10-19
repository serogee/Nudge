package com.modukey.app.core

enum class HoldExecution { ON_RELEASE, AFTER_TIME }

data class GestureFeedback(val enabled: Boolean = false, val strength: Float = .5f) {
    fun normalized() = copy(strength = (if (strength.isFinite()) strength else .5f).coerceIn(.1f, 1f))
}
