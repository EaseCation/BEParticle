package net.easecation.beparticle.emitter

import net.easecation.beparticle.definition.component.*
import org.joml.Vector3f
import kotlin.math.*
import kotlin.random.Random

/**
 * Resolves emitter shapes — computes spawn position and direction for new particles.
 */
object EmitterShapeResolver {

    data class SpawnResult(
        val offsetX: Float, val offsetY: Float, val offsetZ: Float,
        val dirX: Float, val dirY: Float, val dirZ: Float
    )

    fun resolvePoint(shape: EmitterShapePoint, vars: Map<String, Float>): SpawnResult {
        val ox = evalExpr(shape.offset[0], vars)
        val oy = evalExpr(shape.offset[1], vars)
        val oz = evalExpr(shape.offset[2], vars)
        val (dx, dy, dz) = resolveDirection(shape.direction, ox, oy, oz, vars)
        return SpawnResult(ox, oy, oz, dx, dy, dz)
    }

    fun resolveSphere(shape: EmitterShapeSphere, vars: Map<String, Float>): SpawnResult {
        val ox = evalExpr(shape.offset[0], vars)
        val oy = evalExpr(shape.offset[1], vars)
        val oz = evalExpr(shape.offset[2], vars)
        val radius = evalExpr(shape.radius, vars)

        // Random point on/in sphere
        val theta = Random.nextFloat() * 2f * PI.toFloat()
        val phi = acos(1f - 2f * Random.nextFloat())
        val r = if (shape.surfaceOnly) radius else radius * cbrt(Random.nextFloat().toDouble()).toFloat()

        val sx = r * sin(phi) * cos(theta)
        val sy = r * cos(phi)
        val sz = r * sin(phi) * sin(theta)

        val (dx, dy, dz) = resolveDirection(shape.direction, sx, sy, sz, vars)
        return SpawnResult(ox + sx, oy + sy, oz + sz, dx, dy, dz)
    }

    fun resolveBox(shape: EmitterShapeBox, vars: Map<String, Float>): SpawnResult {
        val ox = evalExpr(shape.offset[0], vars)
        val oy = evalExpr(shape.offset[1], vars)
        val oz = evalExpr(shape.offset[2], vars)
        val hx = evalExpr(shape.halfDimensions[0], vars)
        val hy = evalExpr(shape.halfDimensions[1], vars)
        val hz = evalExpr(shape.halfDimensions[2], vars)

        val sx: Float
        val sy: Float
        val sz: Float
        if (shape.surfaceOnly) {
            // Random point on box surface
            val face = Random.nextInt(6)
            sx = when (face) {
                0 -> hx; 1 -> -hx; else -> Random.nextFloat() * 2f * hx - hx
            }
            sy = when (face) {
                2 -> hy; 3 -> -hy; else -> if (face < 2) Random.nextFloat() * 2f * hy - hy else Random.nextFloat() * 2f * hy - hy
            }
            sz = when (face) {
                4 -> hz; 5 -> -hz; else -> if (face < 4) Random.nextFloat() * 2f * hz - hz else Random.nextFloat() * 2f * hz - hz
            }
        } else {
            sx = Random.nextFloat() * 2f * hx - hx
            sy = Random.nextFloat() * 2f * hy - hy
            sz = Random.nextFloat() * 2f * hz - hz
        }

        val (dx, dy, dz) = resolveDirection(shape.direction, sx, sy, sz, vars)
        return SpawnResult(ox + sx, oy + sy, oz + sz, dx, dy, dz)
    }

    fun resolveDisc(shape: EmitterShapeDisc, vars: Map<String, Float>): SpawnResult {
        val ox = evalExpr(shape.offset[0], vars)
        val oy = evalExpr(shape.offset[1], vars)
        val oz = evalExpr(shape.offset[2], vars)
        val radius = evalExpr(shape.radius, vars)

        // Plane normal
        val nx = evalExpr(shape.planeNormal[0], vars)
        val ny = evalExpr(shape.planeNormal[1], vars)
        val nz = evalExpr(shape.planeNormal[2], vars)

        // Build orthonormal basis on the disc plane
        val (tangent, bitangent) = buildPlaneBasis(nx, ny, nz)

        // Random point on/in disc
        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val r = if (shape.surfaceOnly) radius else radius * sqrt(Random.nextFloat())
        val lx = r * cos(angle)
        val ly = r * sin(angle)

        val sx = tangent.x * lx + bitangent.x * ly
        val sy = tangent.y * lx + bitangent.y * ly
        val sz = tangent.z * lx + bitangent.z * ly

        val (dx, dy, dz) = resolveDirection(shape.direction, sx, sy, sz, vars)
        return SpawnResult(ox + sx, oy + sy, oz + sz, dx, dy, dz)
    }

    private fun buildPlaneBasis(nx: Float, ny: Float, nz: Float): Pair<Vector3f, Vector3f> {
        val normal = Vector3f(nx, ny, nz)
        val len = normal.length()
        if (len < 0.0001f) {
            return Pair(Vector3f(1f, 0f, 0f), Vector3f(0f, 0f, 1f))
        }
        normal.div(len)

        // Choose a vector not parallel to normal
        val up = if (abs(normal.y) < 0.99f) Vector3f(0f, 1f, 0f) else Vector3f(1f, 0f, 0f)
        val tangent = Vector3f()
        normal.cross(up, tangent).normalize()
        val bitangent = Vector3f()
        normal.cross(tangent, bitangent).normalize()
        return Pair(tangent, bitangent)
    }

    private fun resolveDirection(
        dir: EmitterDirection, px: Float, py: Float, pz: Float,
        vars: Map<String, Float>
    ): Triple<Float, Float, Float> {
        return when (dir) {
            is EmitterDirection.Outward -> normalize(px, py, pz)
            is EmitterDirection.Inward -> normalize(-px, -py, -pz)
            is EmitterDirection.Custom -> Triple(
                evalExpr(dir.direction[0], vars),
                evalExpr(dir.direction[1], vars),
                evalExpr(dir.direction[2], vars)
            )
        }
    }

    private fun normalize(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        val len = sqrt(x * x + y * y + z * z)
        return if (len < 0.0001f) {
            // Random direction for zero-length
            val theta = Random.nextFloat() * 2f * PI.toFloat()
            val phi = acos(1f - 2f * Random.nextFloat())
            Triple(sin(phi) * cos(theta), cos(phi), sin(phi) * sin(theta))
        } else {
            Triple(x / len, y / len, z / len)
        }
    }

    private fun evalExpr(expr: String, vars: Map<String, Float>): Float {
        return net.easecation.beparticle.molang.ParticleMoLang.eval(expr, vars)
    }
}
