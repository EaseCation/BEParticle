# BEParticle

基岩版粒子引擎库，用于 Minecraft Java 版。解析基岩版粒子 JSON 定义，在客户端渲染 Billboard 粒子，完整支持 MoLang 表达式。

作为 Fabric Mod 库构建，供其他需要基岩版风格粒子效果的 Mod 使用。

## 功能特性

- 完整的基岩版粒子 JSON 解析（`particle_effect` 格式）
- MoLang 表达式求值（基于 [mocha](https://github.com/unnamed/mocha)），带解析缓存
- 发射器生命周期：`once`（单次）、`looping`（循环）、`expression`（表达式控制）
- 生成速率：`instant`（瞬时爆发）和 `steady`（持续发射）
- 发射器形状：`point`（点）、`sphere`（球）、`box`（盒）、`disc`（圆盘），支持仅表面生成和方向控制
- 粒子运动：`dynamic`（加速度/阻力）和 `parametric`（MoLang 驱动位置）
- Billboard 渲染，8 种朝向模式（`rotate_xyz`、`lookat_xyz`、`lookat_y`、`direction_x/z`、`emitter_transform_xy/xz/yz`）
- UV：静态和 Flipbook（动画精灵图，支持 FPS 控制、拉伸至生命周期、循环）
- 颜色着色：静态 RGBA、MoLang 表达式、渐变插值（支持十六进制颜色）
- 曲线系统：`linear`（线性）、`bezier`（贝塞尔）、`catmull_rom` 插值，用于驱动变量随时间变化
- 发射器初始化表达式和每帧更新表达式
- 实体绑定 — 将发射器附着到实体上
- 性能优化：
  - 对象池（`ParticlePool`）减少 GC 压力
  - 基于距离的 Tick LOD（近距离：20 TPS，中距离：10 TPS，远距离：5 TPS）
  - 软/硬粒子数量限制，基于距离的淘汰机制
  - MoLang 解析缓存（最多 4096 个表达式）
  - 可复用变量 Map，减少内存分配

## 支持版本

| Minecraft | 状态 |
|-----------|------|
| 1.21.5    | ✅ 自定义 Billboard 渲染器 |
| 1.21.6    | ✅ 自定义 Billboard 渲染器 |
| 1.21.7    | ✅ 自定义 Billboard 渲染器 |
| 1.21.8    | ✅ 自定义 Billboard 渲染器 |
| 1.21.10   | ✅ 原版 `BillboardParticle` 管线 |
| 1.21.11   | ✅ 原版 `BillboardParticle` 管线（VCS 版本） |

多版本构建由 [Stonecutter](https://github.com/kikugie/stonecutter) 驱动。

## 环境要求

- Java 21+
- Fabric Loader ≥ 0.18.0
- Fabric API
- Fabric Language Kotlin

## 使用方法

### 作为依赖引入

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

主要入口是 `ParticleManager` — 一个管理粒子定义和发射器的单例对象。

#### 加载粒子定义

```kotlin
import net.easecation.beparticle.ParticleManager

// 从 JSON 字符串加载（基岩版 particle_effect 格式）
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

// 或注册预解析的定义
ParticleManager.loadDefinition("mymod:my_particle", definition)
```

#### 生成粒子

```kotlin
import org.joml.Vector3f

// 基本生成
ParticleManager.spawnEmitter("mymod:my_particle", Vector3f(x, y, z))

// 带自定义 MoLang 变量
ParticleManager.spawnEmitter(
    "mymod:my_particle",
    Vector3f(x, y, z),
    molangVars = mapOf("speed" to 5.0f, "scale" to 2.0f)
)
```

#### 实体绑定

```kotlin
val emitter = ParticleManager.spawnEmitter("mymod:trail", Vector3f(x, y, z))
emitter?.bindToEntity(entity.id)  // 发射器每帧跟随实体位置
```

#### 配置参数

```kotlin
// 全局粒子数量限制
ParticleManager.globalMaxParticles = 10000
ParticleManager.softMaxParticles = 5000   // 触发基于距离的淘汰
ParticleManager.hardMaxParticles = 10000  // 强制移除最远的粒子

// 渲染距离
ParticleManager.maxRenderDistance = 64f    // 方块

// Tick LOD（基于距离的 Tick 频率降低）
ParticleManager.particleTickLodEnabled = true
ParticleManager.particleTickLodNearDistance = 24f   // 完整 20 TPS
ParticleManager.particleTickLodFarDistance = 48f    // 降低至 5 TPS
```

#### 清理

```kotlin
ParticleManager.clear()          // 移除所有定义和发射器
ParticleManager.clearEmitters()  // 仅移除发射器，保留定义
```

## 架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        ParticleManager                          │
│       （全局 API：加载定义、生成发射器、Tick、淘汰）              │
└──────────┬──────────────────────────────────┬───────────────────┘
           │                                  │
     ┌─────▼─────┐                   ┌────────▼────────┐
     │  定义注册表 │                   │ ParticleEmitter │ ×N
     │            │                   │  （每个实例）     │
     └─────┬─────┘                   └────────┬────────┘
           │                                  │
  ┌────────▼─────────┐              ┌─────────▼──────────┐
  │ParticleJsonParser │              │  Particle（对象池） │ ×M
  │（JSON → Definition）│             │  （可变状态）       │
  └──────────────────┘              └─────────┬──────────┘
                                              │
                              ┌───────────────┼───────────────┐
                              ▼               ▼               ▼
                      ┌──────────┐    ┌──────────┐    ┌──────────┐
                      │ 运动系统  │    │ 外观系统  │    │ 颜色系统  │
                      │(Dynamic/ │    │(Billboard │    │(Static/  │
                      │Parametric)│    │ UV/Flip) │    │ Gradient)│
                      └──────────┘    └──────────┘    └──────────┘
```

### 核心模块

| 模块 | 路径 | 职责 |
|------|------|------|
| `ParticleManager` | `ParticleManager.kt` | 全局 API，定义注册表，发射器生命周期，粒子淘汰 |
| `ParticleJsonParser` | `definition/ParticleJsonParser.kt` | 基岩版 JSON → `ParticleDefinition` 数据类 |
| `ParticleDefinition` | `definition/ParticleDefinition.kt` | 不可变数据类，包含所有解析后的组件 |
| `ParticleEmitter` | `emitter/ParticleEmitter.kt` | 发射器实例，管理生命周期、生成速率、粒子模拟 |
| `EmitterShapeResolver` | `emitter/EmitterShapeResolver.kt` | 计算各形状的生成位置和方向 |
| `Particle` | `element/Particle.kt` | 单个粒子的可变状态（位置、速度、颜色、UV） |
| `ParticlePool` | `element/ParticlePool.kt` | 对象池（最大 4096），减少 GC 压力 |
| `ParticleMoLang` | `molang/ParticleMoLang.kt` | MoLang 解析和求值，带缓存 |
| `LayeredScope` | `molang/LayeredScope.kt` | 分层作用域，避免深拷贝 |
| `CurveEvaluator` | `curve/CurveEvaluator.kt` | 曲线插值（linear/bezier/catmull-rom） |
| `BedrockParticleManager` | `render/BedrockParticleManager.kt` | 同步基岩粒子到原版粒子管线（≥1.21.10） |
| `BedrockBillboardParticle` | `render/BedrockBillboardParticle.kt` | 原版 `BillboardParticle` 包装器（≥1.21.10） |
| `BillboardRenderer` | `render/BillboardRenderer.kt` | 自定义 Billboard 渲染（<1.21.9） |

### 渲染策略

项目根据 Minecraft 版本使用两种渲染路径：

**≥ 1.21.10：原版粒子管线**
- `BedrockParticleManager` 每 tick 将基岩粒子状态同步到原版 `BillboardParticle` 实例
- `BedrockBillboardParticle` 包装器覆盖 UV 坐标以支持基岩版的自定义 UV 系统
- 渲染完全由原版粒子管线处理

**< 1.21.9：自定义渲染**
- `ParticleRenderer` 挂载到 `WorldRenderEvents.AFTER_TRANSLUCENT`
- `BillboardRenderer` 手动计算 Billboard 四边形顶点并提交到 `VertexConsumer`
- 支持所有 8 种朝向模式的自定义矩阵计算

### 发射器 Tick 流程

```
ParticleManager.tick()
  └─ 对每个 ParticleEmitter:
       1. 距离 LOD 检查（跳过远距离发射器的部分 tick）
       2. 初始化（首次 tick 执行 creation_expression）
       3. 执行 per_update_expression
       4. 求值曲线 → 注入为 MoLang 变量
       5. 更新发射器生命周期（once/looping/expression）
       6. 生成新粒子（instant 或 steady 速率）
       7. 更新每个粒子：
          a. 求值运动（dynamic 加速度/阻力 或 parametric 位置）
          b. 求值外观（Billboard 尺寸、UV/Flipbook 帧）
          c. 求值颜色（静态/表达式/渐变插值）
          d. 推进年龄，检查死亡条件
       8. 移除死亡粒子（回收到对象池）
       9. 移除死亡发射器
  └─ 全局淘汰：超过硬限制时移除最远的粒子
```

### 支持的组件

#### 发射器组件

| 组件 | 说明 |
|------|------|
| `minecraft:emitter_initialization` | 创建时和每帧执行的 MoLang 表达式 |
| `minecraft:emitter_lifetime_once` | 单次激活，持续指定时间 |
| `minecraft:emitter_lifetime_looping` | 循环激活，支持休眠时间 |
| `minecraft:emitter_lifetime_expression` | MoLang 表达式控制激活/过期 |
| `minecraft:emitter_rate_instant` | 激活时瞬间生成指定数量粒子 |
| `minecraft:emitter_rate_steady` | 以固定速率持续生成粒子 |
| `minecraft:emitter_shape_point` | 从单点生成 |
| `minecraft:emitter_shape_sphere` | 从球体内/表面生成 |
| `minecraft:emitter_shape_box` | 从盒体内/表面生成 |
| `minecraft:emitter_shape_disc` | 从圆盘内/边缘生成 |

#### 粒子组件

| 组件 | 说明 |
|------|------|
| `minecraft:particle_initial_speed` | 初始速度（标量或向量） |
| `minecraft:particle_initial_spin` | 初始旋转和旋转速率 |
| `minecraft:particle_lifetime_expression` | 最大生命周期和过期表达式 |
| `minecraft:particle_motion_dynamic` | 线性加速度、阻力、旋转加速度 |
| `minecraft:particle_motion_parametric` | MoLang 驱动的相对位置和旋转 |
| `minecraft:particle_appearance_billboard` | 尺寸、朝向模式、UV（静态/Flipbook） |
| `minecraft:particle_appearance_tinting` | 颜色着色（静态/表达式/渐变） |
| `minecraft:particle_appearance_lighting` | 启用光照（标记组件） |

### MoLang 变量

表达式中可用的内置变量：

| 变量 | 说明 |
|------|------|
| `variable.emitter_age` | 发射器创建后的时间（秒） |
| `variable.emitter_lifetime` | 发射器激活持续时间 |
| `variable.emitter_random_1..4` | 每个发射器的随机浮点数 [0, 1) |
| `variable.particle_age` | 粒子生成后的时间（秒） |
| `variable.particle_lifetime` | 粒子最大生命周期 |
| `variable.particle_random_1..4` | 每个粒子的随机浮点数 [0, 1) |

自定义变量可通过 `spawnEmitter()` 的 `molangVars` 参数注入，也可通过 `emitter_initialization` 表达式设置。

## 构建

```bash
# 构建当前活动版本（1.21.11）
./gradlew build

# 构建所有版本（chiseled build）
./gradlew chiseledBuild

# 发布到 mavenLocal
./gradlew publishToMavenLocal
```

输出 JAR：`build/libs/beparticle-mc{version}-1.0.0.jar`

## 许可证

[CC0-1.0](LICENSE) — 公共领域

## 致谢

- **作者：** NaKeR, [EaseCation](https://github.com/EaseCation)
- **MoLang 引擎：** [mocha](https://github.com/unnamed/mocha) by team.unnamed
