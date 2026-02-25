package net.easecation.beparticle.definition.component

/**
 * Particle component data classes — pure data, no logic.
 * Parsed from Bedrock particle JSON.
 */

// ============================================================
// Particle Initial State
// ============================================================

/** minecraft:particle_initial_speed — can be scalar or vector */
data class ParticleInitialSpeed(
    val speed: String = "0" // scalar: applied along direction; or use vector form
)

/** minecraft:particle_initial_spin */
data class ParticleInitialSpin(
    val rotation: String = "0",
    val rotationRate: String = "0"
)

// ============================================================
// Particle Lifetime
// ============================================================

/** minecraft:particle_lifetime_expression */
data class ParticleLifetimeExpression(
    val maxLifetime: String = "1",
    val expirationExpression: String? = null
)

// ============================================================
// Particle Motion
// ============================================================

/** minecraft:particle_motion_dynamic */
data class ParticleMotionDynamic(
    val linearAcceleration: List<String> = listOf("0", "0", "0"),
    val linearDragCoefficient: String = "0",
    val rotationAcceleration: String = "0",
    val rotationDragCoefficient: String = "0"
)

/** minecraft:particle_motion_parametric */
data class ParticleMotionParametric(
    val relativePosition: List<String>? = null,
    val direction: List<String>? = null,
    val rotation: String? = null
)

// ============================================================
// Particle Appearance
// ============================================================

enum class FacingCameraMode {
    ROTATE_XYZ,
    LOOKAT_XYZ,
    LOOKAT_Y,
    DIRECTION_X,
    DIRECTION_Z,
    EMITTER_TRANSFORM_XY,
    EMITTER_TRANSFORM_XZ,
    EMITTER_TRANSFORM_YZ
}

/** UV definition — static or flipbook */
sealed class ParticleUV {
    data class Static(
        val textureWidth: Int = 1,
        val textureHeight: Int = 1,
        val uv: List<String> = listOf("0", "0"),
        val uvSize: List<String> = listOf("1", "1")
    ) : ParticleUV()

    data class Flipbook(
        val baseUV: List<String> = listOf("0", "0"),
        val sizeUV: List<Float> = listOf(1f, 1f),
        val stepUV: List<Float> = listOf(1f, 0f),
        val framesPerSecond: Float = 8f,
        val maxFrame: String = "1",
        val stretchToLifetime: Boolean = false,
        val loop: Boolean = false,
        val textureWidth: Int = 128,
        val textureHeight: Int = 128
    ) : ParticleUV()
}

/** minecraft:particle_appearance_billboard */
data class ParticleAppearanceBillboard(
    val size: List<String> = listOf("0.1", "0.1"),
    val facingCameraMode: FacingCameraMode = FacingCameraMode.ROTATE_XYZ,
    val uv: ParticleUV = ParticleUV.Static()
)

/** minecraft:particle_appearance_tinting — color can be static, expression, or gradient */
sealed class ParticleAppearanceTinting {
    data class StaticColor(
        val color: List<String> = listOf("1", "1", "1", "1") // RGBA, MoLang or float
    ) : ParticleAppearanceTinting()

    data class GradientColor(
        val interpolant: String = "variable.particle_age / variable.particle_lifetime",
        val gradient: Map<Float, List<Float>> = emptyMap() // position -> RGBA
    ) : ParticleAppearanceTinting()
}

/** minecraft:particle_appearance_lighting — empty component, just enables lighting */
class ParticleAppearanceLighting
