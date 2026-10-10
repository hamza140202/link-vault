package com.momostack.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppearancePreferences(
    val themeMode: String = "System", // "System", "Light", "Dark"
    val dynamicColor: Boolean = true,
    val cardDensity: String = "Comfortable", // "Compact", "Comfortable", "Spacious"
    val showThumbnails: Boolean = true,
    val showDomain: Boolean = true,
    val showDescription: Boolean = true,
    val smoothAnimations: Boolean = true
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("momostack_user_prefs", Context.MODE_PRIVATE)

    private val _appearance = MutableStateFlow(loadAppearance())
    val appearance: StateFlow<AppearancePreferences> = _appearance.asStateFlow()

    private fun loadAppearance(): AppearancePreferences {
        return AppearancePreferences(
            themeMode = prefs.getString(KEY_THEME_MODE, "System") ?: "System",
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            cardDensity = prefs.getString(KEY_CARD_DENSITY, "Comfortable") ?: "Comfortable",
            showThumbnails = prefs.getBoolean(KEY_SHOW_THUMBNAILS, true),
            showDomain = prefs.getBoolean(KEY_SHOW_DOMAIN, true),
            showDescription = prefs.getBoolean(KEY_SHOW_DESCRIPTION, true),
            smoothAnimations = prefs.getBoolean(KEY_SMOOTH_ANIMATIONS, true)
        )
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _appearance.value = _appearance.value.copy(themeMode = mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _appearance.value = _appearance.value.copy(dynamicColor = enabled)
    }

    fun setCardDensity(density: String) {
        prefs.edit().putString(KEY_CARD_DENSITY, density).apply()
        _appearance.value = _appearance.value.copy(cardDensity = density)
    }

    fun setShowThumbnails(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_THUMBNAILS, show).apply()
        _appearance.value = _appearance.value.copy(showThumbnails = show)
    }

    fun setShowDomain(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_DOMAIN, show).apply()
        _appearance.value = _appearance.value.copy(showDomain = show)
    }

    fun setShowDescription(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_DESCRIPTION, show).apply()
        _appearance.value = _appearance.value.copy(showDescription = show)
    }

    fun setSmoothAnimations(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMOOTH_ANIMATIONS, enabled).apply()
        _appearance.value = _appearance.value.copy(smoothAnimations = enabled)
    }

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "pref_dynamic_color"
        private const val KEY_CARD_DENSITY = "pref_card_density"
        private const val KEY_SHOW_THUMBNAILS = "pref_show_thumbnails"
        private const val KEY_SHOW_DOMAIN = "pref_show_domain"
        private const val KEY_SHOW_DESCRIPTION = "pref_show_description"
        private const val KEY_SMOOTH_ANIMATIONS = "pref_smooth_animations"
    }
}
