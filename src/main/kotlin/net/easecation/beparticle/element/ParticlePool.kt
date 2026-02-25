package net.easecation.beparticle.element

/**
 * Simple object pool for Particle instances to reduce GC pressure.
 * Not thread-safe — intended for single-thread (client tick) use only.
 */
object ParticlePool {
    private const val MAX_POOL_SIZE = 4096

    private val pool = ArrayDeque<Particle>(256)

    fun obtain(): Particle {
        val p = pool.removeLastOrNull() ?: Particle()
        p.reset()
        return p
    }

    fun free(particle: Particle) {
        if (pool.size < MAX_POOL_SIZE) {
            pool.addLast(particle)
        }
    }

    fun clear() {
        pool.clear()
    }

    val poolSize: Int get() = pool.size
}
