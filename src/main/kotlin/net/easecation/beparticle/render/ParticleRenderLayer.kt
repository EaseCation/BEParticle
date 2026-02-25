package net.easecation.beparticle.render

import net.minecraft.client.render.RenderLayer
//? if >=1.21.11 {
import net.minecraft.client.render.RenderLayers
//?} else {
/*import net.minecraft.client.render.RenderLayer as RenderLayerFactory
*///?}
import net.minecraft.util.Identifier

/**
 * Provides RenderLayer instances for particle rendering.
 * Uses entityTranslucent for alpha-blended textured quads.
 */
object ParticleRenderLayer {

    private val cache = mutableMapOf<Identifier, RenderLayer>()

    fun getTranslucent(texture: Identifier): RenderLayer {
        return cache.getOrPut(texture) {
            //? if >=1.21.11 {
            RenderLayers.entityTranslucent(texture, true)
            //?} else {
            /*RenderLayer.getEntityTranslucent(texture, true)
            *///?}
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
