package net.easecation.beparticle.render

import net.easecation.beparticle.ParticleManager
import net.easecation.beparticle.definition.component.FacingCameraMode
//? if >=1.21.10 {
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
//?} elif <1.21.9 {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
*///?}
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.BufferAllocator
import net.minecraft.util.Identifier

/**
 * Entry point for particle world rendering.
 * Registered on WorldRenderEvents.BEFORE_TRANSLUCENT (>=1.21.9)
 * or WorldRenderEvents.AFTER_TRANSLUCENT (<1.21.9).
 */
object ParticleRenderer {

    // Default texture for particles without explicit texture
    private val DEFAULT_TEXTURE = Identifier.of("minecraft", "textures/particle/generic_0")
    private var renderCount = 0L

    fun render(context: WorldRenderContext) {
        renderCount++
        val emitters = ParticleManager.getEmitters()
        if (emitters.isEmpty()) return

        if (renderCount % 100 == 0L) {
            val totalParticles = emitters.sumOf { it.getParticles().size }
            net.easecation.beparticle.BedrockParticleLib.logger.info(
                "[Particle:L8] render#{}: {} emitters, {} particles",
                renderCount, emitters.size, totalParticles
            )
        }

        // Get camera position
        //? if >=1.21.10 {
        val camState = context.worldState().cameraRenderState
        val camX = camState.pos.x
        val camY = camState.pos.y
        val camZ = camState.pos.z
        //?} else {
        /*val camera = context.camera()
        val camPos = camera.pos
        val camX = camPos.x
        val camY = camPos.y
        val camZ = camPos.z
        *///?}

        // Get tick delta
        val client = MinecraftClient.getInstance()
        val tickDelta = client.renderTickCounter.getTickProgress(false)

        // Get consumers
        //? if >=1.21.10 {
        val consumers = context.consumers()
        //?} else {
        /*val consumers: VertexConsumerProvider? = context.consumers()
        *///?}

        val maxDistSq = (ParticleManager.maxRenderDistance * ParticleManager.maxRenderDistance).toDouble()

        // If consumers is available, use it; otherwise create our own
        //? if >=1.21.10 {
        val vertexConsumers = consumers
        //?} else {
        /*val allocator = BufferAllocator(256 * 1024)
        val immediate = VertexConsumerProvider.immediate(allocator)
        val vertexConsumers: VertexConsumerProvider = consumers ?: immediate
        *///?}

        for (emitter in emitters) {
            val particles = emitter.getParticles()
            if (particles.isEmpty()) continue

            // Emitter-level distance culling (early exit for far emitters)
            val emitterDx = emitter.position.x - camX
            val emitterDy = emitter.position.y - camY
            val emitterDz = emitter.position.z - camZ
            val emitterDistSq = emitterDx * emitterDx + emitterDy * emitterDy + emitterDz * emitterDz
            // Use 2x max render distance for emitter check (particles may spread beyond emitter pos)
            if (emitterDistSq > maxDistSq * 4.0) continue

            // Determine texture from definition
            val texturePath = emitter.definition.texture
            val texture = if (texturePath.isNotEmpty()) {
                Identifier.of(texturePath)
            } else {
                DEFAULT_TEXTURE
            }

            val facing = emitter.definition.particleAppearanceBillboard?.facingCameraMode
                ?: FacingCameraMode.ROTATE_XYZ

            val renderLayer = ParticleRenderLayer.getTranslucent(texture)
            val consumer = vertexConsumers.getBuffer(renderLayer)

            for (particle in particles) {
                // Distance culling
                val dx = particle.posX - camX
                val dy = particle.posY - camY
                val dz = particle.posZ - camZ
                val distSq = dx * dx + dy * dy + dz * dz
                if (distSq > maxDistSq) continue

                BillboardRenderer.render(
                    consumer, particle,
                    camX, camY, camZ,
                    tickDelta, facing
                )
            }
        }

        // Flush if we created our own immediate
        //? if <1.21.9 {
        /*if (consumers == null) {
            immediate.draw()
            allocator.close()
        }
        *///?}
    }
}
