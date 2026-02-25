package net.easecation.beparticle.render

//? if >=1.21.10 {
import net.easecation.beparticle.element.Particle as BedrockParticle
import net.minecraft.client.particle.BillboardParticle
import net.minecraft.client.texture.Sprite
import net.minecraft.client.world.ClientWorld

/**
 * Wraps a Bedrock particle as a vanilla BillboardParticle.
 * tick() is empty — lifecycle/motion/color driven by ParticleEmitter.
 * UV overrides sprite UV for Bedrock's custom UV system (flipbook etc).
 */
class BedrockBillboardParticle(
    world: ClientWorld,
    x: Double, y: Double, z: Double,
    sprite: Sprite,
    private val renderType: BillboardParticle.RenderType
) : BillboardParticle(world, x, y, z, sprite) {

    // Custom UV (overrides sprite)
    var customMinU = 0f
    var customMaxU = 1f
    var customMinV = 0f
    var customMaxV = 1f

    init {
        // Disable vanilla gravity and collision
        this.gravityStrength = 0f
        this.collidesWithWorld = false
        // Long max age — death controlled by syncFromBedrock
        this.maxAge = Int.MAX_VALUE
    }

    /**
     * Sync state from Bedrock particle each tick.
     */
    fun syncFromBedrock(bp: BedrockParticle) {
        this.lastX = bp.prevPosX
        this.lastY = bp.prevPosY
        this.lastZ = bp.prevPosZ
        this.x = bp.posX
        this.y = bp.posY
        this.z = bp.posZ
        this.red = bp.colorR
        this.green = bp.colorG
        this.blue = bp.colorB
        this.alpha = bp.colorA
        this.scale = bp.sizeX * 0.5f
        this.zRotation = bp.rotation
        this.lastZRotation = bp.rotation
        // Sync UV
        this.customMinU = bp.uvU
        this.customMaxU = bp.uvU + bp.uvSizeU
        this.customMinV = bp.uvV
        this.customMaxV = bp.uvV + bp.uvSizeV
        // Sync death
        if (bp.dead) {
            this.markDead()
        }
    }

    override fun getMinU() = customMinU
    override fun getMaxU() = customMaxU
    override fun getMinV() = customMinV
    override fun getMaxV() = customMaxV

    override fun getRenderType() = renderType

    override fun tick() {
        // Empty — driven by ParticleEmitter
    }
}
//?}
