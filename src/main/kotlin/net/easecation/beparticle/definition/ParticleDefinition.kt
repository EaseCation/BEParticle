package net.easecation.beparticle.definition

import net.easecation.beparticle.definition.component.*

/**
 * A parsed Bedrock particle effect definition.
 * Contains all components extracted from the JSON.
 */
data class ParticleDefinition(
    val identifier: String,
    /** Material type: particles_alpha, particles_blend, particles_opaque, etc. */
    val material: String = "particles_alpha",
    /** Texture path (Bedrock format, e.g. "textures/particle/particles") */
    val texture: String = "textures/particle/particles",

    // --- Emitter components ---
    val emitterInitialization: EmitterInitialization? = null,

    // Lifetime (mutually exclusive)
    val emitterLifetimeOnce: EmitterLifetimeOnce? = null,
    val emitterLifetimeLooping: EmitterLifetimeLooping? = null,
    val emitterLifetimeExpression: EmitterLifetimeExpression? = null,

    // Rate (mutually exclusive)
    val emitterRateInstant: EmitterRateInstant? = null,
    val emitterRateSteady: EmitterRateSteady? = null,

    // Shape (mutually exclusive)
    val emitterShapePoint: EmitterShapePoint? = null,
    val emitterShapeSphere: EmitterShapeSphere? = null,
    val emitterShapeBox: EmitterShapeBox? = null,
    val emitterShapeDisc: EmitterShapeDisc? = null,

    // --- Particle components ---
    val particleInitialSpeed: ParticleInitialSpeed? = null,
    val particleInitialSpin: ParticleInitialSpin? = null,
    val particleLifetimeExpression: ParticleLifetimeExpression? = null,
    val particleMotionDynamic: ParticleMotionDynamic? = null,
    val particleMotionParametric: ParticleMotionParametric? = null,
    val particleAppearanceBillboard: ParticleAppearanceBillboard? = null,
    val particleAppearanceTinting: ParticleAppearanceTinting? = null,
    val particleAppearanceLighting: ParticleAppearanceLighting? = null,

    // --- Curves ---
    val curves: Map<String, CurveDefinition> = emptyMap()
)
