package net.easecation.beparticle

//? if >=1.21.10 {
import net.easecation.beparticle.render.BedrockParticleManager
//?} elif <1.21.9 {
/*import net.easecation.beparticle.render.ParticleRenderer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
*///?}
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.slf4j.LoggerFactory

object BedrockParticleLib : ClientModInitializer {
    val logger = LoggerFactory.getLogger("beparticle")

    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.world != null) {
                ParticleManager.tick()
                //? if >=1.21.10 {
                BedrockParticleManager.sync()
                //?}
            }
        }

        //? if <1.21.9 {
        /*WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            ParticleRenderer.render(context)
        }
        *///?}

        logger.info("BEParticle initialized")
    }
}
