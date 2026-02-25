package net.easecation.beparticle

import net.easecation.beparticle.definition.ParticleDefinition
import net.easecation.beparticle.definition.ParticleJsonParser
import net.easecation.beparticle.element.ParticlePool
import net.easecation.beparticle.emitter.ParticleEmitter
import net.minecraft.client.MinecraftClient
import org.joml.Vector3f

/**
 * Global particle system API entry point.
 */
object ParticleManager {
    /** Max total particles across all emitters */
    var globalMaxParticles: Int = 10000
    /** Soft limit — triggers distance-based culling of new spawns */
    var softMaxParticles: Int = 5000
    /** Hard limit — force-remove farthest particles when exceeded */
    var hardMaxParticles: Int = 10000
    /** Max render distance in blocks */
    var maxRenderDistance: Float = 64f

    private val definitions = mutableMapOf<String, ParticleDefinition>()
    private val emitters = mutableListOf<ParticleEmitter>()
    private var tickCount = 0L

    /**
     * Register a particle definition from JSON string.
     */
    fun loadDefinition(identifier: String, json: String) {
        try {
            val definition = ParticleJsonParser.parse(json)
            definitions[identifier] = definition
            BedrockParticleLib.logger.debug("Loaded particle definition: {}", identifier)
        } catch (e: Exception) {
            BedrockParticleLib.logger.warn("Failed to parse particle definition: {}", identifier, e)
        }
    }

    /**
     * Register a pre-parsed particle definition.
     */
    fun loadDefinition(identifier: String, definition: ParticleDefinition) {
        definitions[identifier] = definition
    }

    /**
     * Get a registered particle definition by identifier.
     */
    fun getDefinition(identifier: String): ParticleDefinition? = definitions[identifier]

    /**
     * Get the number of registered particle definitions.
     */
    fun getDefinitionCount(): Int = definitions.size

    /**
     * Spawn a new particle emitter at the given position.
     * @return the created emitter, or null if definition not found
     */
    fun spawnEmitter(
        identifier: String,
        position: Vector3f,
        molangVars: Map<String, Float>? = null
    ): ParticleEmitter? {
        val definition = definitions[identifier] ?: run {
            BedrockParticleLib.logger.info("[Particle:L5] Definition NOT FOUND: '{}' (registered: {} definitions: {})", identifier, definitions.size, definitions.keys.take(10))
            return null
        }

        val emitter = ParticleEmitter(definition, Vector3f(position), molangVars)
        emitters.add(emitter)
        BedrockParticleLib.logger.info("[Particle:L5] Emitter created: '{}' at ({}, {}, {}), total emitters: {}", identifier, position.x, position.y, position.z, emitters.size)
        return emitter
    }

    /**
     * Spawn a new particle emitter bound to an entity.
     * The emitter will follow the entity's position each tick.
     */
    fun spawnEmitter(
        identifier: String,
        position: Vector3f,
        entityId: Int,
        molangVars: Map<String, Float>? = null
    ): ParticleEmitter? {
        val emitter = spawnEmitter(identifier, position, molangVars) ?: return null
        emitter.bindToEntity(entityId)
        return emitter
    }

    /**
     * Tick all active emitters. Called once per client tick (20 tps).
     */
    fun tick() {
        tickCount++
        val totalParticles = emitters.sumOf { it.particleCount }
        val overSoftLimit = totalParticles > softMaxParticles

        if (tickCount % 100 == 0L && emitters.isNotEmpty()) {
            BedrockParticleLib.logger.info("[Particle:L6] tick#{}: {} emitters, {} total particles", tickCount, emitters.size, totalParticles)
        }

        val iterator = emitters.iterator()
        while (iterator.hasNext()) {
            val emitter = iterator.next()
            if (emitter.isDead) {
                iterator.remove()
                continue
            }
            emitter.tick(overSoftLimit)
        }

        // Hard limit enforcement: force-remove farthest particles
        if (totalParticles > hardMaxParticles) {
            enforceHardLimit()
        }
    }

    /**
     * When over hard limit, sort emitters by distance to camera (farthest first)
     * and kill particles from the farthest emitters until under limit.
     */
    private fun enforceHardLimit() {
        val player = MinecraftClient.getInstance().player ?: return
        val camX = player.x
        val camY = player.y
        val camZ = player.z

        // Sort emitters by distance descending
        val sorted = emitters.sortedByDescending { e ->
            val dx = e.position.x - camX
            val dy = e.position.y - camY
            val dz = e.position.z - camZ
            dx * dx + dy * dy + dz * dz
        }

        var excess = getTotalParticleCount() - hardMaxParticles
        for (emitter in sorted) {
            if (excess <= 0) break
            val killed = emitter.killFarthestParticles(excess, camX, camY, camZ)
            excess -= killed
        }
    }

    /**
     * Get all active emitters for rendering.
     */
    fun getEmitters(): List<ParticleEmitter> = ArrayList(emitters)

    /**
     * Get total particle count across all emitters.
     */
    fun getTotalParticleCount(): Int = emitters.sumOf { it.particleCount }

    /**
     * Clear all definitions and emitters.
     */
    fun clear() {
        definitions.clear()
        emitters.clear()
        ParticlePool.clear()
    }

    /**
     * Clear all active emitters but keep definitions.
     */
    fun clearEmitters() {
        emitters.clear()
        ParticlePool.clear()
    }
}
