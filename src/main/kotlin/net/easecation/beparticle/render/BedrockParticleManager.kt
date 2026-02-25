package net.easecation.beparticle.render

//? if >=1.21.10 {
import net.easecation.beparticle.BedrockParticleLib
import net.easecation.beparticle.ParticleManager
import net.easecation.beparticle.element.Particle as BedrockParticle
import net.minecraft.client.MinecraftClient
import net.minecraft.client.particle.BillboardParticle
import net.minecraft.client.texture.Sprite
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.util.Identifier
import java.util.IdentityHashMap

/**
 * Bridges Bedrock particles to vanilla BillboardParticle instances.
 * Each tick, syncs Bedrock particle state → vanilla particle.
 * Rendering is handled entirely by the vanilla particle pipeline.
 */
object BedrockParticleManager {

    private val renderTypeCache = mutableMapOf<Identifier, BillboardParticle.RenderType>()
    private val particleMap = IdentityHashMap<BedrockParticle, BedrockBillboardParticle>()
    private var dummySprite: Sprite? = null
    private var syncCount = 0L

    private fun getDummySprite(): Sprite {
        return dummySprite ?: run {
            val atlasManager = MinecraftClient.getInstance().atlasManager
            val atlas = atlasManager.getAtlasTexture(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
            val sprite = atlas.getMissingSprite()
            dummySprite = sprite
            sprite
        }
    }

    private fun getRenderType(texture: Identifier): BillboardParticle.RenderType {
        return renderTypeCache.getOrPut(texture) {
            //? if >=1.21.11 {
            BillboardParticle.RenderType(
                true,
                texture,
                net.minecraft.client.gl.RenderPipelines.TRANSLUCENT_PARTICLE
            )
            //?} else {
            /*BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT
            *///?}
        }
    }

    private fun resolveTexture(texturePath: String): Identifier {
        return if (texturePath.isNotEmpty()) {
            Identifier.of(texturePath)
        } else {
            Identifier.of("minecraft", "textures/particle/particles")
        }
    }

    /**
     * Sync all Bedrock particles to vanilla particle instances.
     * Called once per client tick after ParticleManager.tick().
     */
    fun sync() {
        val client = MinecraftClient.getInstance()
        val world = client.world ?: return
        val vanillaParticleManager = client.particleManager
        val sprite = getDummySprite()

        syncCount++
        val aliveSet = IdentityHashMap<BedrockParticle, Boolean>()

        for (emitter in ParticleManager.getEmitters()) {
            val texture = resolveTexture(emitter.definition.texture)
            val renderType = getRenderType(texture)

            for (bp in emitter.getParticles()) {
                if (bp.dead) continue
                aliveSet[bp] = true

                var vp = particleMap[bp]
                if (vp == null || !vp.isAlive) {
                    vp = BedrockBillboardParticle(world, bp.posX, bp.posY, bp.posZ, sprite, renderType)
                    particleMap[bp] = vp
                    vanillaParticleManager.addParticle(vp)
                }

                vp.syncFromBedrock(bp)
            }
        }

        // Remove dead mappings
        val iter = particleMap.entries.iterator()
        while (iter.hasNext()) {
            val (bp, vp) = iter.next()
            if (!aliveSet.containsKey(bp)) {
                vp.markDead()
                iter.remove()
            }
        }

        if (syncCount % 100 == 0L && particleMap.isNotEmpty()) {
            BedrockParticleLib.logger.info(
                "[Particle:Sync] sync#{}: {} vanilla particles mapped",
                syncCount, particleMap.size
            )
        }
    }

    fun clear() {
        particleMap.values.forEach { it.markDead() }
        particleMap.clear()
        renderTypeCache.clear()
        dummySprite = null
    }
}
//?}
