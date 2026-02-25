package net.easecation.beparticle.render

//? if <1.21.9 {
/*import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

object ParticleRenderLayer {

    private val cache = mutableMapOf<Identifier, RenderLayer>()

    fun getTranslucent(texture: Identifier): RenderLayer {
        return cache.getOrPut(texture) {
            RenderLayer.getEntityTranslucent(texture, true)
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
*///?}
