# BEParticle

A Bedrock Edition particle engine library for Minecraft Java Edition. Parses Bedrock particle JSON definitions and renders billboard particles client-side with full MoLang expression support.

Built as a Fabric mod library — designed to be consumed by other mods that need Bedrock-style particle effects.

## Features

- Full Bedrock particle JSON parsing (`particle_effect` format)
- MoLang expression evaluation (via [mocha](https://github.com/unnamed/mocha)) with parse caching
- Emitter lifecycle: `once`, `looping`, `expression` modes
- Spawn rate: `instant` burst and `steady` continuous emission
- Emitter shapes: `point`, `sphere`, `box`, `disc` (with surface-only and direction control)
- Particle motion: `dynamic` (acceleration/drag) and `parametric` (MoLang-driven position)
- Billboard rendering with 8 facing modes (`rotate_xyz`, `lookat_xyz`, `lookat_y`, `direction_x/z`, `emitter_transform_xy/xz/yz`)
- UV: static and flipbook (animated sprite sheets with FPS control, stretch-to-lifetime, looping)
- Color tinting: static RGBA, MoLang expressions, and gradient interpolation (with hex color support)
- Curve system: `linear`, `bezier`, `catmull_rom` interpolation for driving variables over time
- Emitter initialization expressions and per-update expressions
- Entity binding — attach emitters to entities
- Performance optimizations:
  - Object pooling (`ParticlePool`) to reduce GC pressure
  - Distance-based tick LOD (near: 20 TPS, mid: 10 TPS, far: 5 TPS)
  - Soft/hard particle limits with distance-based culling
  - MoLang parse cache (up to 4096 expressions)
  - Reusable variable maps to minimize allocations

## Supported Versions

| Minecraft | Status |
|-----------|--------|
| 1.21.5    | ✅ Custom billboard renderer |
| 1.21.6    | ✅ Custom billboard renderer |
| 1.21.7    | ✅ Custom billboard renderer |
| 1.21.8    | ✅ Custom billboard renderer |
| 1.21.10   | ✅ Vanilla `BillboardParticle` pipeline |
| 1.21.11   | ✅ Vanilla `BillboardParticle` pipeline (VCS version) |

Multi-version builds powered by [Stonecutter](https://github.com/kikugie/stonecutter).

## Requirements

- Java 21+
- Fabric Loader ≥ 0.18.0
- Fabric API
- Fabric Language Kotlin

## Usage

### As a Dependency

```groovy
// build.gradle
repositories {
    mavenLocal()
}

dependencies {
    modImplementation "net.easecation.beparticle:beparticle:1.0.0"
}
```

### API

The primary entry point is `ParticleManager` — a singleton object that manages definitions and emitters.

#### Loading Particle Definitions

```kotlin
import net.easecation.beparticle.ParticleManager

// From JSON string (Bedrock particle_effect format)
val json = """
{
  "format_version": "1.10.0",
  "particle_effect": {
    "description": {
      "identifier": "mymod:my_particle",
      "basic_render_parameters": {
        "material": "particles_alpha",
        "texture": "textures/particle/my_texture"
      }
    },
    "components": {
      "minecraft:emitter_lifetime_once": { "active_time": 2 },
      "minecraft:emitter_rate_steady": { "spawn_rate": 10, "max_particles": 50 },
      "minecraft:emitter_shape_sphere": { "radius": 1.5 },
      "minecraft:particle_lifetime_expression": { "max_lifetime": 1.5 },
      "minecraft:particle_initial_speed": "3",
      "minecraft:particle_motion_dynamic": {
        "linear_acceleration": [0, -5, 0]
      },
      "minecraft:particle_appearance_billboard": {
        "size": [0.1, 0.1],
        "facing_camera_mode": "rotate_xyz",
        "uv": { "texture_width": 128, "texture_height": 128, "uv": [0, 0], "uv_size": [8, 8] }
      }
    }
  }
}
"""
ParticleManager.loadDefinition("mymod:my_particle", json)

// Or register a pre-parsed definition
ParticleManager.loadDefinition("mymod:my_particle", definition)
```

#### Spawning Particles

```kotlin
import org.joml.Vector3f

// Basic spawn
ParticleManager.spawnEmitter("mymod:my_particle", Vector3f(x, y, z))

// With custom MoLang variables
ParticleManager.spawnEmitter(
    "mymod:my_particle",
    Vector3f(x, y, z),
    molangVars = mapOf("speed" to 5.0f, "scale" to 2.0f)
)
```

#### Entity Binding

```kotlin
val emitter = ParticleManager.spawnEmitter("mymod:trail", Vector3f(x, y, z))
emitter?.bindToEntity(entity.id)  // Emitter follows the entity each tick
```

#### Configuration

```kotlin
// Global particle limits
ParticleManager.globalMaxParticles = 10000
ParticleManager.softMaxParticles = 5000   // Triggers distance-based culling
ParticleManager.hardMaxParticles = 10000  // Force-removes farthest particles

// Render distance
ParticleManager.maxRenderDistance = 64f    // Blocks

// Tick LOD (distance-based tick rate reduction)
ParticleManager.particleTickLodEnabled = true
ParticleManager.particleTickLodNearDistance = 24f   // Full 20 TPS
ParticleManager.particleTickLodFarDistance = 48f    // Reduced to 5 TPS
```

#### Cleanup

```kotlin
ParticleManager.clear()          // Remove all definitions + emitters
ParticleManager.clearEmitters()  // Remove emitters, keep definitions
```

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        ParticleManager                          │
│  (Global API: load definitions, spawn emitters, tick, cull)     │
└──────────┬──────────────────────────────────┬───────────────────┘
           │                                  │
     ┌─────▼─────┐                   ┌────────▼────────┐
     │ Definition │                   │ ParticleEmitter │ ×N
     │  Registry  │                   │  (per instance) │
     └─────┬─────┘                   └────────┬────────┘
           │                                  │
  ┌────────▼─────────┐              ┌─────────▼──────────┐
  │ParticleJsonParser │              │   Particle (pool)  │ ×M
  │ (JSON → Definition)│              │ (mutable state)    │
  └──────────────────┘              └─────────┬──────────┘
                                              │
                              ┌───────────────┼───────────────┐
                              │               │               │
                     ┌────────▼──┐   ┌────────▼──┐   ┌───────▼────────┐
                     │ParticleMoLang│ │CurveEvaluator│ │EmitterShapeResolver│
                     │(eval+cache)│   │(interpolate)│   │(spawn position)│
                     └───────────┘   └────────────┘   └────────────────┘
                                              │
                              ┌───────────────┴───────────────┐
                              │          Rendering            │
                              ├───────────────────────────────┤
                              │ ≥1.21.10: BedrockParticleManager  │
                              │   → BedrockBillboardParticle      │
                              │   → Vanilla particle pipeline     │
                              │                               │
                              │ <1.21.9:  ParticleRenderer        │
                              │   → BillboardRenderer             │
                              │   → Custom world render hook      │
                              └───────────────────────────────┘
```

### Core Components

| Component | File | Role |
|-----------|------|------|
| `ParticleManager` | `ParticleManager.kt` | Global singleton API. Manages definition registry, active emitters, tick loop, and particle culling. |
| `ParticleDefinition` | `definition/ParticleDefinition.kt` | Immutable data class holding all parsed components of a Bedrock particle effect. |
| `ParticleJsonParser` | `definition/ParticleJsonParser.kt` | Parses Bedrock `particle_effect` JSON into `ParticleDefinition`. Handles all component types, UV modes, color formats. |
| `ParticleEmitter` | `emitter/ParticleEmitter.kt` | Per-instance emitter. Manages lifecycle (once/looping/expression), spawn rate (instant/steady), spawns particles via shape resolver, ticks all owned particles. |
| `Particle` | `element/Particle.kt` | Mutable state of a single particle: position, velocity, rotation, color, UV, lifetime. Pooled for reuse. |
| `ParticlePool` | `element/ParticlePool.kt` | Lock-free object pool (max 4096) for `Particle` instances. Single-thread (client tick) only. |
| `ParticleMoLang` | `molang/ParticleMoLang.kt` | MoLang evaluator with parse caching (ConcurrentHashMap, max 4096). Fast path for numeric literals. Thread-local scope reuse. |
| `LayeredScope` | `molang/LayeredScope.kt` | Lightweight scope layering — local writes on top of parent scope without deep copy. |
| `CurveEvaluator` | `curve/CurveEvaluator.kt` | Evaluates curve definitions: linear, cubic bezier (4 control points), Catmull-Rom spline. |
| `EmitterShapeResolver` | `emitter/EmitterShapeResolver.kt` | Computes spawn position + direction for point/sphere/box/disc shapes. Handles surface-only and inward/outward/custom directions. |

### Rendering Pipeline

**≥ 1.21.10 (Vanilla Integration)**

`BedrockParticleManager` bridges Bedrock particles into the vanilla `BillboardParticle` pipeline:
1. Each tick, after `ParticleManager.tick()`, `BedrockParticleManager.sync()` is called
2. For each alive Bedrock `Particle`, a `BedrockBillboardParticle` wrapper is created (or reused via `IdentityHashMap`)
3. The wrapper syncs position, color, scale, rotation, and UV from the Bedrock particle state
4. Rendering is handled entirely by vanilla's particle renderer — no custom render hooks needed
5. Dead Bedrock particles trigger `markDead()` on their vanilla counterparts

**< 1.21.9 (Custom Renderer)**

`ParticleRenderer` hooks into `WorldRenderEvents.AFTER_TRANSLUCENT`:
1. Groups particles by texture into render layers (`ParticleRenderLayer`)
2. `BillboardRenderer` computes billboard axes based on facing mode, applies rotation, and emits quad vertices
3. Custom vertex submission with position, color, UV, overlay, light, and normal

### Supported Bedrock Components

**Emitter Components:**
- `minecraft:emitter_initialization` — creation + per-update MoLang expressions
- `minecraft:emitter_lifetime_once` / `looping` / `expression`
- `minecraft:emitter_rate_instant` / `steady`
- `minecraft:emitter_shape_point` / `sphere` / `box` / `disc`

**Particle Components:**
- `minecraft:particle_initial_speed` — scalar applied along spawn direction
- `minecraft:particle_initial_spin` — initial rotation + rotation rate
- `minecraft:particle_lifetime_expression` — max lifetime + expiration expression
- `minecraft:particle_motion_dynamic` — linear acceleration, drag, rotation acceleration/drag
- `minecraft:particle_motion_parametric` — MoLang-driven relative position, direction, rotation
- `minecraft:particle_appearance_billboard` — size, facing mode, UV (static/flipbook)
- `minecraft:particle_appearance_tinting` — static color, MoLang color, gradient
- `minecraft:particle_appearance_lighting` — enables lighting (marker component)

**Curves:**
- `linear` — piecewise linear interpolation across N nodes
- `bezier` — cubic Bezier with 4 control points
- `catmull_rom` — Catmull-Rom spline (first/last nodes as control points)

### MoLang Variables

Built-in variables available in expressions:

| Variable | Description |
|----------|-------------|
| `variable.emitter_age` | Time since emitter creation (seconds) |
| `variable.emitter_lifetime` | Emitter active duration |
| `variable.emitter_random_1..4` | Per-emitter random floats [0, 1) |
| `variable.particle_age` | Time since particle spawn (seconds) |
| `variable.particle_lifetime` | Particle max lifetime |
| `variable.particle_random_1..4` | Per-particle random floats [0, 1) |

Custom variables can be injected via `molangVars` parameter in `spawnEmitter()`, and via `emitter_initialization` expressions.

## Building

```bash
# Build for active version (1.21.11)
./gradlew build

# Build for all versions (chiseled build)
./gradlew chiseledBuild

# Publish to mavenLocal
./gradlew publishToMavenLocal
```

Output JARs: `build/libs/beparticle-mc{version}-1.0.0.jar`

## License

[MIT](LICENSE)

## Credits

- **Authors:** [EaseCation](https://github.com/EaseCation)
- **MoLang Engine:** [mocha](https://github.com/unnamed/mocha) by team.unnamed
