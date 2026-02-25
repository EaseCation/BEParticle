package net.easecation.beparticle.element

import org.joml.Vector3f
import kotlin.random.Random

/**
 * A single particle instance. Mutable state updated each tick.
 */
class Particle {
    // Position & motion
    var posX: Double = 0.0
    var posY: Double = 0.0
    var posZ: Double = 0.0
    var prevPosX: Double = 0.0
    var prevPosY: Double = 0.0
    var prevPosZ: Double = 0.0
    var speedX: Float = 0f
    var speedY: Float = 0f
    var speedZ: Float = 0f

    // Lifetime
    var age: Float = 0f
    var lifetime: Float = 1f
    var dead: Boolean = false

    // Appearance
    var sizeX: Float = 0.1f
    var sizeY: Float = 0.1f
    var rotation: Float = 0f
    var rotationRate: Float = 0f
    var colorR: Float = 1f
    var colorG: Float = 1f
    var colorB: Float = 1f
    var colorA: Float = 1f

    // UV
    var uvU: Float = 0f
    var uvV: Float = 0f
    var uvSizeU: Float = 1f
    var uvSizeV: Float = 1f

    // Per-particle random values (set once at creation)
    var random1: Float = 0f
    var random2: Float = 0f
    var random3: Float = 0f
    var random4: Float = 0f

    fun reset() {
        posX = 0.0; posY = 0.0; posZ = 0.0
        prevPosX = 0.0; prevPosY = 0.0; prevPosZ = 0.0
        speedX = 0f; speedY = 0f; speedZ = 0f
        age = 0f; lifetime = 1f; dead = false
        sizeX = 0.1f; sizeY = 0.1f
        rotation = 0f; rotationRate = 0f
        colorR = 1f; colorG = 1f; colorB = 1f; colorA = 1f
        uvU = 0f; uvV = 0f; uvSizeU = 1f; uvSizeV = 1f
        random1 = Random.nextFloat()
        random2 = Random.nextFloat()
        random3 = Random.nextFloat()
        random4 = Random.nextFloat()
    }

    fun initRandoms() {
        random1 = Random.nextFloat()
        random2 = Random.nextFloat()
        random3 = Random.nextFloat()
        random4 = Random.nextFloat()
    }
}
