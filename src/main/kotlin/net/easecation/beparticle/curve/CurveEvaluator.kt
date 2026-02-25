package net.easecation.beparticle.curve

import net.easecation.beparticle.definition.CurveDefinition
import net.easecation.beparticle.definition.CurveType
import net.easecation.beparticle.molang.ParticleMoLang

/**
 * Evaluates curve definitions. Maps an input value through a set of nodes
 * using the specified interpolation type.
 */
object CurveEvaluator {

    /**
     * Evaluate a curve given the current MoLang variable context.
     * @return the interpolated output value
     */
    fun evaluate(curve: CurveDefinition, vars: Map<String, Float>): Float {
        if (curve.nodes.isEmpty()) return 0f

        val input = ParticleMoLang.eval(curve.input, vars)
        val range = ParticleMoLang.eval(curve.horizontalRange, vars)
        val t = if (range != 0f) (input / range).coerceIn(0f, 1f) else 0f

        return when (curve.type) {
            CurveType.LINEAR -> evalLinear(curve.nodes, t)
            CurveType.BEZIER -> evalBezier(curve.nodes, t)
            CurveType.CATMULL_ROM -> evalCatmullRom(curve.nodes, t)
            CurveType.BEZIER_CHAIN -> evalLinear(curve.nodes, t) // fallback to linear for now
        }
    }

    /**
     * Linear interpolation across N nodes evenly spaced over [0, 1].
     */
    private fun evalLinear(nodes: List<Float>, t: Float): Float {
        if (nodes.size < 2) return nodes.firstOrNull() ?: 0f

        val scaledT = t * (nodes.size - 1)
        val leftIndex = scaledT.toInt().coerceIn(0, nodes.size - 2)
        val rightIndex = leftIndex + 1
        val frac = scaledT - leftIndex

        return nodes[leftIndex] + frac * (nodes[rightIndex] - nodes[leftIndex])
    }

    /**
     * Cubic Bezier with exactly 4 control points.
     * B(t) = (1-t)^3*P0 + 3*(1-t)^2*t*P1 + 3*(1-t)*t^2*P2 + t^3*P3
     */
    private fun evalBezier(nodes: List<Float>, t: Float): Float {
        if (nodes.size != 4) return evalLinear(nodes, t) // fallback

        val p0 = nodes[0]
        val p1 = nodes[1]
        val p2 = nodes[2]
        val p3 = nodes[3]
        val u = 1f - t

        return u * u * u * p0 +
               3f * u * u * t * p1 +
               3f * u * t * t * p2 +
               t * t * t * p3
    }

    /**
     * Catmull-Rom spline. Requires at least 4 nodes.
     * First and last nodes are control points (not interpolated through).
     */
    private fun evalCatmullRom(nodes: List<Float>, t: Float): Float {
        if (nodes.size < 4) return evalLinear(nodes, t) // fallback

        // Effective interpolation range is between nodes[1] and nodes[n-2]
        val effectiveSize = nodes.size - 2
        val scaledT = t * (effectiveSize - 1)
        val p1Index = (scaledT.toInt() + 1).coerceIn(1, nodes.size - 3)
        val p0Index = p1Index - 1
        val p2Index = p1Index + 1
        val p3Index = p1Index + 2

        val p0 = nodes[p0Index]
        val p1 = nodes[p1Index]
        val p2 = nodes[p2Index]
        val p3 = nodes[p3Index]

        val frac = scaledT - (p1Index - 1)

        // Catmull-Rom matrix form
        return 0.5f * (
            (2f * p1) +
            (-p0 + p2) * frac +
            (2f * p0 - 5f * p1 + 4f * p2 - p3) * frac * frac +
            (-p0 + 3f * p1 - 3f * p2 + p3) * frac * frac * frac
        )
    }
}
