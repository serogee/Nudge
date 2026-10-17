package com.modukey.app.core

/** Ordered ownership. Failed specifications retry after a change or explicit reconnect. */
class OverlayRegistry<K, V>(private val host: (K) -> OverlayHost<V>) {
    private val windows = linkedMapOf<K, SingleOverlay<V>>()
    private val failed = mutableMapOf<K, V>()
    val attached: Set<K> get() = windows.keys.toSet()
    fun reconcile(desired: Map<K, V>, raise: Boolean = false): Map<K, RuntimeException> {
        val errors = linkedMapOf<K, RuntimeException>()
        windows.keys.filter { it !in desired }.toList().forEach { id -> try { windows.remove(id)?.hide() } catch (e: RuntimeException) { errors[id] = e } }
        failed.keys.retainAll(desired.keys)
        val expected = desired.filter { (id, value) -> failed[id] != value }.keys.toList()
        // Keep the correctly ordered prefix. Only an insertion or reorder requires raising later windows.
        val keep = if (raise) 0 else windows.keys.zip(expected).takeWhile { (current, wanted) -> current == wanted }.size
        windows.keys.drop(keep).toList().forEach { id -> try { windows.remove(id)?.hide() } catch (e: RuntimeException) { errors[id] = e } }
        for ((id, value) in desired) {
            if (failed[id] == value) continue
            val window = windows[id] ?: SingleOverlay(host(id))
            try { window.show(value); windows[id] = window; failed.remove(id) }
            catch (e: RuntimeException) { windows.remove(id); failed[id] = value; errors[id] = e; try { window.hide() } catch (_: RuntimeException) { } }
        }
        return errors
    }
    fun clear() = reconcile(emptyMap())
}
