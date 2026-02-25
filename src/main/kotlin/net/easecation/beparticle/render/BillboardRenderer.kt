package net.easecation.beparticle.render

//? if <1.21.9 {
/*import net.easecation.beparticle.definition.component.FacingCameraMode
import net.easecation.beparticle.element.Particle
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumer
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object BillboardRenderer {

    private val right = Vector3f()
    private val up = Vector3f()
    private val v0 = Vector3f()
    private val v1 = Vector3f()
    private val v2 = Vector3f()
    private val v3 = Vector3f()

    fun render(
        consumer: VertexConsumer,
        particle: Particle,
        camX: Double, camY: Double, camZ: Double,
        tickDelta: Float,
        facing: FacingCameraMode
    ) {
        val px = (lerp(tickDelta, particle.prevPosX, particle.posX) - camX).toFloat()
        val py = (lerp(tickDelta, particle.prevPosY, particle.posY) - camY).toFloat()
        val pz = (lerp(tickDelta, particle.prevPosZ, particle.posZ) - camZ).toFloat()
        if (px.isNaN() || py.isNaN() || pz.isNaN()) return

        val halfW = particle.sizeX * 0.5f
        val halfH = particle.sizeY * 0.5f
        computeBillboardAxes(facing, px, py, pz, particle, halfW, halfH)
        if (particle.rotation != 0f) applyRotation(particle.rotation)

        v0.set(px).add(right.x * (-halfW) + up.x * (-halfH), right.y * (-halfW) + up.y * (-halfH), right.z * (-halfW) + up.z * (-halfH))
        v1.set(px).add(right.x * halfW + up.x * (-halfH), right.y * halfW + up.y * (-halfH), right.z * halfW + up.z * (-halfH))
        v2.set(px).add(right.x * halfW + up.x * halfH, right.y * halfW + up.y * halfH, right.z * halfW + up.z * halfH)
        v3.set(px).add(right.x * (-halfW) + up.x * halfH, right.y * (-halfW) + up.y * halfH, right.z * (-halfW) + up.z * halfH)

        val u0 = particle.uvU; val v0uv = particle.uvV
        val u1 = particle.uvU + particle.uvSizeU; val v1uv = particle.uvV + particle.uvSizeV
        val color = packColor(particle.colorR, particle.colorG, particle.colorB, particle.colorA)
        val light = LightmapTextureManager.MAX_LIGHT_COORDINATE
        val overlay = OverlayTexture.DEFAULT_UV

        consumer.vertex(v0.x, v0.y, v0.z).color(color).texture(u0, v1uv).overlay(overlay).light(light).normal(0f, 1f, 0f)
        consumer.vertex(v1.x, v1.y, v1.z).color(color).texture(u1, v1uv).overlay(overlay).light(light).normal(0f, 1f, 0f)
        consumer.vertex(v2.x, v2.y, v2.z).color(color).texture(u1, v0uv).overlay(overlay).light(light).normal(0f, 1f, 0f)
        consumer.vertex(v3.x, v3.y, v3.z).color(color).texture(u0, v0uv).overlay(overlay).light(light).normal(0f, 1f, 0f)
    }

    private fun computeBillboardAxes(facing: FacingCameraMode, px: Float, py: Float, pz: Float, particle: Particle, halfW: Float, halfH: Float) {
        when (facing) {
            FacingCameraMode.ROTATE_XYZ, FacingCameraMode.LOOKAT_XYZ -> {
                val dist = sqrt((px * px + py * py + pz * pz).toDouble()).toFloat()
                if (dist < 0.0001f) { right.set(1f, 0f, 0f); up.set(0f, 1f, 0f); return }
                val fx = -px / dist; val fy = -py / dist; val fz = -pz / dist
                var rx = fz; var ry = 0f; var rz = -fx
                val rLen = sqrt((rx * rx + rz * rz).toDouble()).toFloat()
                if (rLen < 0.0001f) { right.set(1f, 0f, 0f); up.set(0f, 0f, -1f); return }
                rx /= rLen; rz /= rLen
                val ux = ry * fz - rz * fy; val uy = rz * fx - rx * fz; val uz = rx * fy - ry * fx
                right.set(rx, ry, rz); up.set(ux, uy, uz)
            }
            FacingCameraMode.LOOKAT_Y -> {
                val dist2D = sqrt((px * px + pz * pz).toDouble()).toFloat()
                if (dist2D < 0.0001f) right.set(1f, 0f, 0f) else right.set(pz / dist2D, 0f, -px / dist2D)
                up.set(0f, 1f, 0f)
            }
            FacingCameraMode.DIRECTION_X, FacingCameraMode.DIRECTION_Z -> {
                val vx = particle.speedX; val vy = particle.speedY; val vz = particle.speedZ
                val vLen = sqrt((vx * vx + vy * vy + vz * vz).toDouble()).toFloat()
                if (vLen > 0.0001f) {
                    up.set(vx / vLen, vy / vLen, vz / vLen)
                    val worldUp = Vector3f(0f, 1f, 0f)
                    if (kotlin.math.abs(up.dot(worldUp)) > 0.99f) worldUp.set(1f, 0f, 0f)
                    up.cross(worldUp, right).normalize()
                }
            }
            else -> { right.set(1f, 0f, 0f); up.set(0f, 1f, 0f) }
        }
    }

    private fun applyRotation(angle: Float) {
        val cosA = cos(angle); val sinA = sin(angle)
        val rx = right.x * cosA + up.x * sinA; val ry = right.y * cosA + up.y * sinA; val rz = right.z * cosA + up.z * sinA
        val ux = -right.x * sinA + up.x * cosA; val uy = -right.y * sinA + up.y * cosA; val uz = -right.z * sinA + up.z * cosA
        right.set(rx, ry, rz); up.set(ux, uy, uz)
    }

    private fun lerp(delta: Float, start: Double, end: Double): Double = start + (end - start) * delta

    private fun packColor(r: Float, g: Float, b: Float, a: Float): Int {
        val ri = (r.coerceIn(0f, 1f) * 255).toInt(); val gi = (g.coerceIn(0f, 1f) * 255).toInt()
        val bi = (b.coerceIn(0f, 1f) * 255).toInt(); val ai = (a.coerceIn(0f, 1f) * 255).toInt()
        return (ai shl 24) or (ri shl 16) or (gi shl 8) or bi
    }
}
*///?}
