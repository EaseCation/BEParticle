package net.easecation.beparticle.definition

/**
 * Curve definition for particle effects.
 * Curves map an input value (typically particle_age/particle_lifetime) to an output value.
 */
data class CurveDefinition(
    val type: CurveType = CurveType.LINEAR,
    val input: String = "variable.particle_age / variable.particle_lifetime",
    val horizontalRange: String = "1",
    val nodes: List<Float> = emptyList()
)

enum class CurveType {
    LINEAR,
    BEZIER,
    CATMULL_ROM,
    BEZIER_CHAIN
}
