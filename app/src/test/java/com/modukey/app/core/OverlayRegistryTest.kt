package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class OverlayRegistryTest {
    @Test fun `updates avoid duplicates and new priorities restore stacking order`() {
        val stack=mutableListOf<String>(); var updates=0
        val registry=OverlayRegistry<String,Int> { id -> object:OverlayHost<Int> {
            override fun attach(value:Int) { check(id !in stack); stack+=id }
            override fun update(value:Int) { updates++ }
            override fun detach() { stack.remove(id) }
        } }
        registry.reconcile(linkedMapOf("first" to 1)); registry.reconcile(linkedMapOf("first" to 1)); assertEquals(listOf("first"),stack)
        registry.reconcile(linkedMapOf("last" to 1,"first" to 1)); assertEquals(listOf("last","first"),stack)
        registry.reconcile(linkedMapOf("last" to 2,"first" to 1)); assertEquals(1,updates)
        registry.reconcile(linkedMapOf("first" to 1,"last" to 2)); assertEquals(listOf("first","last"),stack)
        registry.clear(); registry.clear(); assertTrue(stack.isEmpty())
    }
    @Test fun `one failure leaves other handles available without idle retries`() {
        var attempts=0
        val registry=OverlayRegistry<String,Int> { id -> object:OverlayHost<Int> {
            override fun attach(value:Int) { if(id=="bad") { attempts++; error("blocked") } }
            override fun update(value:Int)=Unit
            override fun detach()=Unit
        } }
        val desired=linkedMapOf("bad" to 1,"good" to 1)
        assertEquals(setOf("bad"),registry.reconcile(desired).keys)
        registry.reconcile(desired); assertEquals(1,attempts); assertEquals(setOf("good"),registry.attached)
        registry.reconcile(desired+ ("bad" to 2)); assertEquals(2,attempts)
    }
    @Test fun `removing a handle and appending above it preserves unaffected windows`() {
        val attached = mutableListOf<String>(); val detached = mutableListOf<String>()
        val registry = OverlayRegistry<String, Int> { id -> object : OverlayHost<Int> {
            override fun attach(value: Int) { attached += id }
            override fun update(value: Int) = Unit
            override fun detach() { detached += id }
        } }
        registry.reconcile(linkedMapOf("low" to 1, "middle" to 1, "top" to 1))
        attached.clear()
        registry.reconcile(linkedMapOf("low" to 1, "top" to 1))
        assertEquals(listOf("middle"), detached); assertTrue(attached.isEmpty())
        registry.reconcile(linkedMapOf("low" to 1, "top" to 1, "new" to 1))
        assertEquals(listOf("new"), attached); assertEquals(listOf("middle"), detached)
    }
    @Test fun `inserting a handle only raises the windows above its position`() {
        val attached = mutableListOf<String>(); val detached = mutableListOf<String>()
        val registry = OverlayRegistry<String, Int> { id -> object : OverlayHost<Int> {
            override fun attach(value: Int) { attached += id }
            override fun update(value: Int) = Unit
            override fun detach() { detached += id }
        } }
        registry.reconcile(linkedMapOf("low" to 1, "top" to 1))
        attached.clear()
        registry.reconcile(linkedMapOf("low" to 1, "middle" to 1, "top" to 1))
        assertEquals(listOf("top"), detached); assertEquals(listOf("middle", "top"), attached)
    }
}
