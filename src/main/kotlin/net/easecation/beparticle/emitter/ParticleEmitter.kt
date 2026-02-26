package net.easecation.beparticle.emitter

import net.easecation.beparticle.ParticleManager
import net.easecation.beparticle.curve.CurveEvaluator
import net.easecation.beparticle.definition.ParticleDefinition
import net.easecation.beparticle.definition.component.*
import net.easecation.beparticle.element.Particle
import net.easecation.beparticle.element.ParticlePool
import net.easecation.beparticle.molang.ParticleMoLang
import net.minecraft.client.MinecraftClient
import org.joml.Vector3f
import kotlin.random.Random

/**
 * A particle emitter instance. Created when SpawnParticleEffect is received.
 * Manages its own lifecycle and all particles it spawns.
 */
class ParticleEmitter(
    val definition: ParticleDefinition,
    val position: Vector3f,
    private val externalMolangVars: Map<String, Float>? = null
) {
    companion object {
        const val DT: Float = 1f / 20f // 20 TPS
        const val SAFETY_MAX_PARTICLES: Int = 20000
    }

    private val particles = mutableListOf<Particle>()
    private var emitterAge: Float = 0f
    private var emitterLifetime: Float = 10f
    private var active: Boolean = true
    private var wasActive: Boolean = true // Track previous active state for looping reset
    private var hasEmittedInstant: Boolean = false

    /** Optional entity ID to follow. Set via [bindToEntity]. */
    var boundEntityId: Int = -1
        private set

    // Per-emitter random values
    private val emitterRandom1 = Random.nextFloat()
    private val emitterRandom2 = Random.nextFloat()
    private val emitterRandom3 = Random.nextFloat()
    private val emitterRandom4 = Random.nextFloat()

    // Steady rate accumulator
    private var spawnAccumulator: Float = 0f

    // Custom variables set by emitter_initialization expressions
    private val customVars = mutableMapOf<String, Float>()
    private var initialized: Boolean = false
    private var firstTickLogged: Boolean = false

    // Reusable vars map to reduce GC pressure
    private val reusableVars = mutableMapOf<String, Float>()
    // Reusable particle vars map (avoids toMutableMap() per-particle per-tick)
    private val reusableParticleVars = mutableMapOf<String, Float>()

    val particleCount: Int get() = particles.size
    val isDead: Boolean get() = !active && particles.isEmpty()

    fun getParticles(): List<Particle> = particles

    /**
     * Kill up to [count] particles, preferring those farthest from camera.
     * @return number of particles actually killed
     */
    fun killFarthestParticles(count: Int, camX: Double, camY: Double, camZ: Double): Int {
        if (particles.isEmpty() || count <= 0) return 0
        val toKill = count.coerceAtMost(particles.size)

        // Sort by distance descending, kill the farthest
        particles.sortByDescending { p ->
            val dx = p.posX - camX
            val dy = p.posY - camY
            val dz = p.posZ - camZ
            dx * dx + dy * dy + dz * dz
        }

        var killed = 0
        val iter = particles.iterator()
        while (iter.hasNext() && killed < toKill) {
            val p = iter.next()
            iter.remove()
            ParticlePool.free(p)
            killed++
        }
        return killed
    }

    /** Bind this emitter to follow an entity by its network ID. */
    fun bindToEntity(entityId: Int) {
        boundEntityId = entityId
    }

    /**
     * Tick this emitter. Called once per client tick.
     * @param overSoftLimit true if global particle count exceeds soft limit
     */
    fun tick(overSoftLimit: Boolean = false) {
        // Follow bound entity position
        if (boundEntityId >= 0) {
            val world = MinecraftClient.getInstance().world
            val entity = world?.getEntityById(boundEntityId)
            if (entity != null) {
                position.set(entity.x.toFloat(), entity.y.toFloat(), entity.z.toFloat())
            } else if (emitterAge > DT * 5) {
                // Entity gone — kill emitter
                active = false
            }
        }

        emitterAge += DT
        val vars = buildEmitterVars()

        // Run initialization expressions
        if (!initialized) {
            definition.emitterInitialization?.creationExpression?.let {
                ParticleMoLang.eval(it, vars)
            }
            initialized = true
        }

        // Log first tick details
        if (!firstTickLogged) {
            firstTickLogged = true
            val lifetimeType = when {
                definition.emitterLifetimeOnce != null -> "once"
                definition.emitterLifetimeLooping != null -> "looping"
                definition.emitterLifetimeExpression != null -> "expression"
                else -> "none"
            }
            val rateType = when {
                definition.emitterRateInstant != null -> "instant"
                definition.emitterRateSteady != null -> "steady"
                else -> "none"
            }
            net.easecation.beparticle.BedrockParticleLib.logger.info(
                "[Particle:L7] Emitter first tick: '{}', lifetime={}, rate={}, active={}, pos=({}, {}, {})",
                definition.identifier, lifetimeType, rateType, active,
                position.x, position.y, position.z
            )
        }

        // Run per-update expressions
        definition.emitterInitialization?.perUpdateExpression?.let {
            ParticleMoLang.eval(it, vars)
        }

        // Update emitter lifetime
        updateEmitterLifetime(vars)

        // Emit new particles
        if (active && !overSoftLimit) {
            val beforeCount = particles.size
            emitParticles(vars)
            val afterCount = particles.size
            if (afterCount > beforeCount) {
                net.easecation.beparticle.BedrockParticleLib.logger.info(
                    "[Particle:L7] Emitted {} particles (total: {}), age={}, lifetime={}",
                    afterCount - beforeCount, afterCount, emitterAge, emitterLifetime
                )
            }
        }

        // Tick existing particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            tickParticle(p, vars)
            if (p.dead) {
                iterator.remove()
                ParticlePool.free(p)
            }
        }
    }

    private fun updateEmitterLifetime(vars: Map<String, Float>) {
        val once = definition.emitterLifetimeOnce
        val looping = definition.emitterLifetimeLooping
        val expr = definition.emitterLifetimeExpression

        when {
            once != null -> {
                emitterLifetime = ParticleMoLang.eval(once.activeTime, vars)
                if (emitterAge >= emitterLifetime) active = false
            }
            looping != null -> {
                val activeTime = ParticleMoLang.eval(looping.activeTime, vars)
                val sleepTime = ParticleMoLang.eval(looping.sleepTime, vars)
                val cycleTime = activeTime + sleepTime
                if (cycleTime > 0) {
                    val phase = emitterAge % cycleTime
                    active = phase < activeTime
                }
                emitterLifetime = activeTime
                // Reset instant emission flag when transitioning from sleep to active
                if (active && !wasActive) {
                    hasEmittedInstant = false
                }
            }
            expr != null -> {
                val activation = ParticleMoLang.eval(expr.activationExpression, vars)
                val expiration = ParticleMoLang.eval(expr.expirationExpression, vars)
                if (activation == 0f) active = false
                if (expiration != 0f) active = false
            }
            else -> {
                // No lifetime component — emit once and die
                if (emitterAge > DT * 2) active = false
            }
        }
        wasActive = active
    }

    private fun emitParticles(vars: Map<String, Float>) {
        val instant = definition.emitterRateInstant
        val steady = definition.emitterRateSteady

        when {
            instant != null -> {
                if (!hasEmittedInstant) {
                    val count = ParticleMoLang.eval(instant.numParticles, vars).toInt()
                        .coerceAtMost(SAFETY_MAX_PARTICLES)
                    repeat(count) { spawnParticle(vars) }
                    hasEmittedInstant = true
                }
            }
            steady != null -> {
                val maxParticles = ParticleMoLang.eval(steady.maxParticles, vars).toInt()
                val spawnRate = ParticleMoLang.eval(steady.spawnRate, vars)
                spawnAccumulator += spawnRate * DT

                while (spawnAccumulator >= 1f && particles.size < maxParticles) {
                    spawnParticle(vars)
                    spawnAccumulator -= 1f
                }
                // Prevent accumulation when at capacity
                if (particles.size >= maxParticles) {
                    spawnAccumulator = 0f
                } else {
                    spawnAccumulator = spawnAccumulator.coerceAtMost(spawnRate)
                }
            }
            else -> {
                // No rate component — emit 1 particle
                if (!hasEmittedInstant) {
                    spawnParticle(vars)
                    hasEmittedInstant = true
                }
            }
        }
    }

    private fun spawnParticle(vars: Map<String, Float>) {
        if (particles.size >= ParticleManager.globalMaxParticles) return

        val p = ParticlePool.obtain()

        // Resolve shape
        val spawn = resolveShape(vars)
        p.posX = position.x.toDouble() + spawn.offsetX
        p.posY = position.y.toDouble() + spawn.offsetY
        p.posZ = position.z.toDouble() + spawn.offsetZ
        p.prevPosX = p.posX
        p.prevPosY = p.posY
        p.prevPosZ = p.posZ

        // Initial speed
        val speed = definition.particleInitialSpeed?.let {
            ParticleMoLang.eval(it.speed, vars)
        } ?: 0f
        p.speedX = spawn.dirX * speed
        p.speedY = spawn.dirY * speed
        p.speedZ = spawn.dirZ * speed

        // Initial spin
        definition.particleInitialSpin?.let {
            p.rotation = ParticleMoLang.eval(it.rotation, vars)
            p.rotationRate = ParticleMoLang.eval(it.rotationRate, vars)
        }

        // Lifetime
        val particleVars = ParticleMoLang.addParticleVars(
            vars.toMutableMap(), 0f, 1f, p.random1, p.random2, p.random3, p.random4
        )
        definition.particleLifetimeExpression?.let {
            p.lifetime = ParticleMoLang.eval(it.maxLifetime, particleVars)
        }

        // Skip zero or negative lifetime particles
        if (p.lifetime <= 0f) {
            ParticlePool.free(p)
            return
        }

        // Billboard size
        definition.particleAppearanceBillboard?.let { bb ->
            p.sizeX = ParticleMoLang.eval(bb.size[0], particleVars)
            p.sizeY = ParticleMoLang.eval(bb.size[1], particleVars)
        }

        // UV
        resolveUV(p)

        // Tinting
        resolveTinting(p, particleVars)

        particles.add(p)
    }

    private fun resolveShape(vars: Map<String, Float>): EmitterShapeResolver.SpawnResult {
        definition.emitterShapePoint?.let { return EmitterShapeResolver.resolvePoint(it, vars) }
        definition.emitterShapeSphere?.let { return EmitterShapeResolver.resolveSphere(it, vars) }
        definition.emitterShapeBox?.let { return EmitterShapeResolver.resolveBox(it, vars) }
        definition.emitterShapeDisc?.let { return EmitterShapeResolver.resolveDisc(it, vars) }
        // Default: point at origin
        return EmitterShapeResolver.SpawnResult(0f, 0f, 0f, 0f, 1f, 0f)
    }

    private fun resolveUV(p: Particle) {
        val bb = definition.particleAppearanceBillboard ?: return
        when (val uv = bb.uv) {
            is ParticleUV.Static -> {
                val texW = uv.textureWidth.toFloat()
                val texH = uv.textureHeight.toFloat()
                p.uvU = uv.uv[0].toFloatOrNull()?.div(texW) ?: 0f
                p.uvV = uv.uv[1].toFloatOrNull()?.div(texH) ?: 0f
                p.uvSizeU = uv.uvSize[0].toFloatOrNull()?.div(texW) ?: 1f
                p.uvSizeV = uv.uvSize[1].toFloatOrNull()?.div(texH) ?: 1f
            }
            is ParticleUV.Flipbook -> {
                val texW = uv.textureWidth.toFloat()
                val texH = uv.textureHeight.toFloat()
                // Frame 0
                val baseU = uv.baseUV[0].toFloatOrNull() ?: 0f
                val baseV = uv.baseUV[1].toFloatOrNull() ?: 0f
                p.uvU = baseU / texW
                p.uvV = baseV / texH
                p.uvSizeU = uv.sizeUV[0] / texW
                p.uvSizeV = uv.sizeUV[1] / texH
            }
        }
    }

    private fun resolveTinting(p: Particle, vars: Map<String, Float>) {
        when (val tinting = definition.particleAppearanceTinting) {
            is ParticleAppearanceTinting.StaticColor -> {
                p.colorR = ParticleMoLang.eval(tinting.color.getOrElse(0) { "1" }, vars)
                p.colorG = ParticleMoLang.eval(tinting.color.getOrElse(1) { "1" }, vars)
                p.colorB = ParticleMoLang.eval(tinting.color.getOrElse(2) { "1" }, vars)
                p.colorA = ParticleMoLang.eval(tinting.color.getOrElse(3) { "1" }, vars)
            }
            is ParticleAppearanceTinting.GradientColor -> {
                // Evaluate at spawn time (will be re-evaluated each tick for dynamic gradients)
                val t = ParticleMoLang.eval(tinting.interpolant, vars).coerceIn(0f, 1f)
                val color = interpolateGradient(tinting.gradient, t)
                p.colorR = color[0]; p.colorG = color[1]; p.colorB = color[2]; p.colorA = color[3]
            }
            null -> {}
        }
    }

    private fun tickParticle(p: Particle, emitterVars: MutableMap<String, Float>) {
        p.age += DT
        if (p.age >= p.lifetime) {
            p.dead = true
            return
        }

        p.prevPosX = p.posX
        p.prevPosY = p.posY
        p.prevPosZ = p.posZ

        // Reuse particle vars map instead of toMutableMap() per-particle per-tick
        reusableParticleVars.clear()
        reusableParticleVars.putAll(emitterVars)
        val vars = ParticleMoLang.addParticleVars(
            reusableParticleVars,
            p.age, p.lifetime, p.random1, p.random2, p.random3, p.random4
        )

        // Check expiration expression
        definition.particleLifetimeExpression?.expirationExpression?.let { expr ->
            val result = ParticleMoLang.eval(expr, vars)
            if (result != 0f) {
                p.dead = true
                return
            }
        }

        // Parametric motion (overrides dynamic if both present)
        val parametric = definition.particleMotionParametric
        if (parametric != null) {
            tickParametricMotion(p, parametric, vars)
        } else {
            // Dynamic motion
            definition.particleMotionDynamic?.let { motion ->
                val accelX = ParticleMoLang.eval(motion.linearAcceleration[0], vars)
                val accelY = ParticleMoLang.eval(motion.linearAcceleration[1], vars)
                val accelZ = ParticleMoLang.eval(motion.linearAcceleration[2], vars)
                val drag = ParticleMoLang.eval(motion.linearDragCoefficient, vars)

                p.speedX += (accelX - p.speedX * drag) * DT
                p.speedY += (accelY - p.speedY * drag) * DT
                p.speedZ += (accelZ - p.speedZ * drag) * DT

                p.posX += p.speedX * DT
                p.posY += p.speedY * DT
                p.posZ += p.speedZ * DT

                // Rotation
                val rotAccel = ParticleMoLang.eval(motion.rotationAcceleration, vars)
                val rotDrag = ParticleMoLang.eval(motion.rotationDragCoefficient, vars)
                p.rotationRate += (rotAccel - p.rotationRate * rotDrag) * DT
            }
        }

        // Parametric rotation (can coexist with dynamic motion)
        parametric?.rotation?.let { rotExpr ->
            p.rotation = ParticleMoLang.eval(rotExpr, vars)
        } ?: run {
            p.rotation += p.rotationRate * DT
        }

        // Update billboard size (may use MoLang with particle_age)
        definition.particleAppearanceBillboard?.let { bb ->
            p.sizeX = ParticleMoLang.eval(bb.size[0], vars)
            p.sizeY = ParticleMoLang.eval(bb.size[1], vars)
        }

        // Update tinting
        resolveTinting(p, vars)

        // Update flipbook UV
        updateFlipbook(p)
    }

    private fun tickParametricMotion(
        p: Particle,
        parametric: ParticleMotionParametric,
        vars: Map<String, Float>
    ) {
        // Relative position: MoLang-driven offset from emitter origin
        parametric.relativePosition?.let { relPos ->
            if (relPos.size >= 3) {
                val rx = ParticleMoLang.eval(relPos[0], vars).toDouble()
                val ry = ParticleMoLang.eval(relPos[1], vars).toDouble()
                val rz = ParticleMoLang.eval(relPos[2], vars).toDouble()
                p.posX = position.x.toDouble() + rx
                p.posY = position.y.toDouble() + ry
                p.posZ = position.z.toDouble() + rz
            }
        }

        // Direction: MoLang-driven velocity direction (normalized, then scaled by speed magnitude)
        parametric.direction?.let { dir ->
            if (dir.size >= 3) {
                val dx = ParticleMoLang.eval(dir[0], vars)
                val dy = ParticleMoLang.eval(dir[1], vars)
                val dz = ParticleMoLang.eval(dir[2], vars)
                val len = kotlin.math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
                if (len > 1e-6f) {
                    val speed = kotlin.math.sqrt(
                        (p.speedX * p.speedX + p.speedY * p.speedY + p.speedZ * p.speedZ).toDouble()
                    ).toFloat()
                    p.speedX = dx / len * speed
                    p.speedY = dy / len * speed
                    p.speedZ = dz / len * speed
                }
            }
        }
    }

    private fun updateFlipbook(p: Particle) {
        val bb = definition.particleAppearanceBillboard ?: return
        val uv = bb.uv as? ParticleUV.Flipbook ?: return

        val maxFrame = uv.maxFrame.toFloatOrNull()?.toInt()?.coerceAtLeast(0) ?: 1
        val frame: Int = if (uv.stretchToLifetime) {
            val progress = if (p.lifetime > 0f) (p.age / p.lifetime).coerceIn(0f, 1f) else 0f
            (progress * (maxFrame + 1)).toInt().coerceAtMost(maxFrame)
        } else {
            val rawFrame = (p.age * uv.framesPerSecond).toInt()
            if (uv.loop && maxFrame > 0) rawFrame % (maxFrame + 1) else rawFrame.coerceAtMost(maxFrame)
        }

        val baseU = uv.baseUV[0].toFloatOrNull() ?: 0f
        val baseV = uv.baseUV[1].toFloatOrNull() ?: 0f
        val texW = uv.textureWidth.toFloat().coerceAtLeast(1f)
        val texH = uv.textureHeight.toFloat().coerceAtLeast(1f)

        p.uvU = (baseU + frame * uv.stepUV[0]) / texW
        p.uvV = (baseV + frame * uv.stepUV[1]) / texH
        p.uvSizeU = uv.sizeUV[0] / texW
        p.uvSizeV = uv.sizeUV[1] / texH
    }

    private fun buildEmitterVars(): MutableMap<String, Float> {
        reusableVars.clear()
        reusableVars["emitter_age"] = emitterAge
        reusableVars["emitter_lifetime"] = emitterLifetime
        reusableVars["emitter_random_1"] = emitterRandom1
        reusableVars["emitter_random_2"] = emitterRandom2
        reusableVars["emitter_random_3"] = emitterRandom3
        reusableVars["emitter_random_4"] = emitterRandom4
        externalMolangVars?.let { reusableVars.putAll(it) }
        reusableVars.putAll(customVars)

        // Evaluate curves and inject results as variables
        for ((name, curve) in definition.curves) {
            val value = CurveEvaluator.evaluate(curve, reusableVars)
            val varName = if (name.startsWith("variable.")) name.removePrefix("variable.") else name
            reusableVars[varName] = value
        }

        return reusableVars
    }

    private fun interpolateGradient(gradient: Map<Float, List<Float>>, t: Float): List<Float> {
        if (gradient.isEmpty()) return listOf(1f, 1f, 1f, 1f)
        val sorted = gradient.entries.sortedBy { it.key }
        if (t <= sorted.first().key) return sorted.first().value
        if (t >= sorted.last().key) return sorted.last().value

        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (t in a.key..b.key) {
                val f = (t - a.key) / (b.key - a.key)
                return List(4) { idx ->
                    val va = a.value.getOrElse(idx) { 1f }
                    val vb = b.value.getOrElse(idx) { 1f }
                    va + (vb - va) * f
                }
            }
        }
        return sorted.last().value
    }
}
