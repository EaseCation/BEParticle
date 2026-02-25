package net.easecation.beparticle

import net.easecation.beparticle.render.ParticleRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
//? if >=1.21.10 {
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
//?} elif <1.21.9 {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
*///?}
import org.slf4j.LoggerFactory

object BedrockParticleLib : ClientModInitializer {
    val logger = LoggerFactory.getLogger("beparticle")

    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.world != null) {
                ParticleManager.tick()
            }
        }

        //? if >=1.21.10 {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register { context ->
            ParticleRenderer.render(context)
        }
        //?} elif <1.21.9 {
        /*WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            ParticleRenderer.render(context)
        }
        *///?}
        // 1.21.9 has no WorldRenderEvents (Fabric API transition), particle rendering unavailable

        logger.info("BEParticle initialized")
    }
}
