package com.modukey.app.data

import com.modukey.app.core.AppConfiguration
import com.modukey.app.core.HandleConfig
import com.modukey.app.core.SavedPanel

/** One editor owns one draft. Applying it never restores a removed item or replaces other items. */
sealed interface ConfigurationDraft {
    val changed: Boolean
    fun applyTo(configuration: AppConfiguration): AppConfiguration
    fun baseline(configuration: AppConfiguration): ConfigurationDraft?

    data class Handle(val original: HandleConfig, val value: HandleConfig = original) : ConfigurationDraft {
        override val changed get() = original != value
        override fun applyTo(configuration: AppConfiguration) = configuration.editHandle(value.id) { value }
        override fun baseline(configuration: AppConfiguration) = configuration.handle(value.id)?.let { Handle(it) }
    }

    data class Panel(val original: SavedPanel, val value: SavedPanel = original) : ConfigurationDraft {
        override val changed get() = original != value
        override fun applyTo(configuration: AppConfiguration) = configuration.editPanel(value.id) { value }
        override fun baseline(configuration: AppConfiguration) = configuration.panel(value.id)?.let { Panel(it) }
    }
}
