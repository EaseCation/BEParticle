package net.easecation.beparticle.definition.component

/**
 * Emitter component data classes — pure data, no logic.
 * Parsed from Bedrock particle JSON.
 * String fields accept MoLang expressions or float literals.
 */

// ============================================================
// Emitter Initialization
// ============================================================

/** minecraft:emitter_initialization */
data class EmitterInitialization(
    val creationExpression: String? = null,
    val perUpdateExpression: String? = null
)

// ============================================================
// Emitter Lifetime
// ============================================================

/** minecraft:emitter_lifetime_once */
data class EmitterLifetimeOnce(
    val activeTime: String = "10"
)

/** minecraft:emitter_lifetime_looping */
data class EmitterLifetimeLooping(
    val activeTime: String = "10",
    val sleepTime: String = "0"
)

/** minecraft:emitter_lifetime_expression */
data class EmitterLifetimeExpression(
    val activationExpression: String = "1",
    val expirationExpression: String = "0"
)

// ============================================================
// Emitter Rate
// ============================================================

/** minecraft:emitter_rate_instant */
data class EmitterRateInstant(
    val numParticles: String = "10"
)

/** minecraft:emitter_rate_steady */
data class EmitterRateSteady(
    val spawnRate: String = "1",
    val maxParticles: String = "50"
)

// ============================================================
// Emitter Shape
// ============================================================

sealed class EmitterDirection {
    data object Outward : EmitterDirection()
    data object Inward : EmitterDirection()
    data class Custom(val direction: List<String>) : EmitterDirection()
}

/** minecraft:emitter_shape_point */
data class EmitterShapePoint(
    val offset: List<String> = listOf("0", "0", "0"),
    val direction: EmitterDirection = EmitterDirection.Outward
)

/** minecraft:emitter_shape_sphere */
data class EmitterShapeSphere(
    val offset: List<String> = listOf("0", "0", "0"),
    val radius: String = "1",
    val surfaceOnly: Boolean = false,
    val direction: EmitterDirection = EmitterDirection.Outward
)

/** minecraft:emitter_shape_box */
data class EmitterShapeBox(
    val offset: List<String> = listOf("0", "0", "0"),
    val halfDimensions: List<String> = listOf("0", "0", "0"),
    val surfaceOnly: Boolean = false,
    val direction: EmitterDirection = EmitterDirection.Outward
)

/** minecraft:emitter_shape_disc */
data class EmitterShapeDisc(
    val offset: List<String> = listOf("0", "0", "0"),
    val radius: String = "1",
    val planeNormal: List<String> = listOf("0", "1", "0"),
    val surfaceOnly: Boolean = false,
    val direction: EmitterDirection = EmitterDirection.Outward
)
