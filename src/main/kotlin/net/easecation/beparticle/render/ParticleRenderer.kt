package net.easecation.beparticle.render

//? if <1.21.9 {
/*import net.easecation.beparticle.ParticleManager
import net.easecation.beparticle.definition.component.FacingCameraMode
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.BufferAllocator
import net.minecraft.util.Identifier

object ParticleRenderer {

    private val DEFAULT_TEXTURE = Identifier.of("minecraft", "textures/particle/generic_0")

    fun render(context: WorldRenderContext) {
        val emitters = ParticleManager.getEmitters()
        if (emitters.isEmpty()) return

        val camera = context.camera()
        val camPos = camera.pos
        val camX = camPos.x; val camY = camPos.y; val camZ = camPos.z

        val client = MinecraftClient.getInstance()
        val tickDelta = client.renderTickCounter.getTickProgress(false)
        val consumers: VertexConsumerProvider? = context.consumers()
        val maxDistSq = (ParticleManager.maxRenderDistance * ParticleManager.maxRenderDistance).toDouble()

        val allocator = BufferAllocator(256 * 1024)
        val immediate = VertexConsumerProvider.immediate(allocator)
        val vertexConsumers: VertexConsumerProvider = consumers ?: immediate

        for (emitter in emitters) {
            val particles = emitter.getParticles()
            if (particles.isEmpty()) continue
            val emitterDx = emitter.position.x - camX; val emitterDy = emitter.position.y - camY; val emitterDz = emitter.position.z - camZ
            if (emitterDx * emitterDx + emitterDy * emitterDy + emitterDz * emitterDz > maxDistSq * 4.0) continue

            val texturePath = emitter.definition.texture
            val texture = if (texturePath.isNotEmpty()) Identifier.of(texturePath) else DEFAULT_TEXTURE
            val facing = emitter.definition.particleAppearanceBillboard?.facingCameraMode ?: FacingCameraMode.ROTATE_XYZ
            val renderLayer = ParticleRenderLayer.getTranslucent(texture)
            val consumer = vertexConsumers.getBuffer(renderLayer)

            for (particle in particles) {
                val dx = particle.posX - camX; val dy = particle.posY - camY; val dz = particle.posZ - camZ
                if (dx * dx + dy * dy + dz * dz > maxDistSq) continue
                BillboardRenderer.render(consumer, particle, camX, camY, camZ, tickDelta, facing)
            }
        }

        if (consumers == null) { immediate.draw(); allocator.close() }
    }
}
*///?}
