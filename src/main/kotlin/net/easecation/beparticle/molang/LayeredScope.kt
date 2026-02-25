package net.easecation.beparticle.molang

import team.unnamed.mocha.runtime.Scope
import team.unnamed.mocha.runtime.value.ObjectProperty
import team.unnamed.mocha.runtime.value.Value

/**
 * Lightweight Scope that layers local bindings on top of a parent scope.
 * Avoids expensive deep-copy. Reads fall through to parent; writes go to local map.
 * (Same pattern as BedrockMotion's LayeredScope)
 */
@Suppress("UnstableApiUsage")
class LayeredScope(private var parent: Scope) : Scope {
    private val local = HashMap<String, ObjectProperty>(4)
    private var isReadOnly = false

    fun reset(newParent: Scope) {
        this.parent = newParent
        this.local.clear()
        this.isReadOnly = false
    }

    override fun getProperty(name: String): ObjectProperty? {
        return local[name] ?: parent.getProperty(name)
    }

    override fun set(name: String, value: Value?): Boolean {
        if (isReadOnly) return false
        if (value == null) {
            local.remove(name)
        } else {
            local[name] = ObjectProperty.property(value, false)
        }
        return true
    }

    override fun copy(): Scope {
        val flat = Scope.create()
        for ((key, prop) in parent.entries()) {
            flat.set(key, prop.value())
        }
        for ((key, prop) in local) {
            flat.set(key, prop.value())
        }
        return flat
    }

    override fun entries(): Map<String, ObjectProperty> {
        val merged = HashMap(parent.entries())
        merged.putAll(local)
        return merged
    }

    override fun readOnly(readOnly: Boolean) {
        this.isReadOnly = readOnly
    }

    override fun readOnly(): Boolean = isReadOnly
}
