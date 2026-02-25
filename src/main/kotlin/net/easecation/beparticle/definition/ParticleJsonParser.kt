package net.easecation.beparticle.definition

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.easecation.beparticle.definition.component.*

/**
 * Parses Bedrock particle JSON into ParticleDefinition.
 */
object ParticleJsonParser {

    fun parse(json: String): ParticleDefinition {
        val root = JsonParser.parseString(json).asJsonObject
        val effect = root.getAsJsonObject("particle_effect")
            ?: throw IllegalArgumentException("Missing particle_effect")
        return parseEffect(effect)
    }

    fun parseEffect(effect: JsonObject): ParticleDefinition {
        val desc = effect.getAsJsonObject("description")
        val identifier = desc?.get("identifier")?.asString ?: "unknown"
        val renderParams = desc?.getAsJsonObject("basic_render_parameters")
        val material = renderParams?.get("material")?.asString ?: "particles_alpha"
        val texture = renderParams?.get("texture")?.asString ?: "textures/particle/particles"

        val components = effect.getAsJsonObject("components") ?: JsonObject()
        val curves = parseCurves(effect.getAsJsonObject("curves"))

        return ParticleDefinition(
            identifier = identifier,
            material = material,
            texture = texture,
            emitterInitialization = parseEmitterInit(components),
            emitterLifetimeOnce = parseLifetimeOnce(components),
            emitterLifetimeLooping = parseLifetimeLooping(components),
            emitterLifetimeExpression = parseLifetimeExpr(components),
            emitterRateInstant = parseRateInstant(components),
            emitterRateSteady = parseRateSteady(components),
            emitterShapePoint = parseShapePoint(components),
            emitterShapeSphere = parseShapeSphere(components),
            emitterShapeBox = parseShapeBox(components),
            emitterShapeDisc = parseShapeDisc(components),
            particleInitialSpeed = parseInitialSpeed(components),
            particleInitialSpin = parseInitialSpin(components),
            particleLifetimeExpression = parseParticleLifetime(components),
            particleMotionDynamic = parseMotionDynamic(components),
            particleMotionParametric = parseMotionParametric(components),
            particleAppearanceBillboard = parseBillboard(components),
            particleAppearanceTinting = parseTinting(components),
            particleAppearanceLighting = if (components.has("minecraft:particle_appearance_lighting"))
                ParticleAppearanceLighting() else null,
            curves = curves
        )
    }

    // --- Helpers ---

    private fun JsonElement.asExpr(): String = if (isJsonPrimitive) asString else toString()

    private fun JsonObject.getExpr(key: String, default: String = "0"): String =
        get(key)?.asExpr() ?: default

    private fun JsonObject.getExprList(key: String, size: Int = 3): List<String> {
        val el = get(key) ?: return List(size) { "0" }
        if (el.isJsonArray) return el.asJsonArray.map { it.asExpr() }
        return List(size) { el.asExpr() }
    }

    private fun parseDirection(obj: JsonObject): EmitterDirection {
        val dir = obj.get("direction") ?: return EmitterDirection.Outward
        if (dir.isJsonPrimitive) {
            return when (dir.asString.lowercase()) {
                "inward" -> EmitterDirection.Inward
                "outward" -> EmitterDirection.Outward
                else -> EmitterDirection.Outward
            }
        }
        if (dir.isJsonArray) {
            return EmitterDirection.Custom(dir.asJsonArray.map { it.asExpr() })
        }
        return EmitterDirection.Outward
    }

    // --- Emitter Initialization ---

    private fun parseEmitterInit(c: JsonObject): EmitterInitialization? {
        val obj = c.getAsJsonObject("minecraft:emitter_initialization") ?: return null
        return EmitterInitialization(
            creationExpression = obj.get("creation_expression")?.asExpr(),
            perUpdateExpression = obj.get("per_update_expression")?.asExpr()
        )
    }

    // --- Emitter Lifetime ---

    private fun parseLifetimeOnce(c: JsonObject): EmitterLifetimeOnce? {
        val obj = c.getAsJsonObject("minecraft:emitter_lifetime_once") ?: return null
        return EmitterLifetimeOnce(activeTime = obj.getExpr("active_time", "10"))
    }

    private fun parseLifetimeLooping(c: JsonObject): EmitterLifetimeLooping? {
        val obj = c.getAsJsonObject("minecraft:emitter_lifetime_looping") ?: return null
        return EmitterLifetimeLooping(
            activeTime = obj.getExpr("active_time", "10"),
            sleepTime = obj.getExpr("sleep_time", "0")
        )
    }

    private fun parseLifetimeExpr(c: JsonObject): EmitterLifetimeExpression? {
        val obj = c.getAsJsonObject("minecraft:emitter_lifetime_expression") ?: return null
        return EmitterLifetimeExpression(
            activationExpression = obj.getExpr("activation_expression", "1"),
            expirationExpression = obj.getExpr("expiration_expression", "0")
        )
    }

    // --- Emitter Rate ---

    private fun parseRateInstant(c: JsonObject): EmitterRateInstant? {
        val obj = c.getAsJsonObject("minecraft:emitter_rate_instant") ?: return null
        return EmitterRateInstant(numParticles = obj.getExpr("num_particles", "10"))
    }

    private fun parseRateSteady(c: JsonObject): EmitterRateSteady? {
        val obj = c.getAsJsonObject("minecraft:emitter_rate_steady") ?: return null
        return EmitterRateSteady(
            spawnRate = obj.getExpr("spawn_rate", "1"),
            maxParticles = obj.getExpr("max_particles", "50")
        )
    }

    // --- Emitter Shape ---

    private fun parseShapePoint(c: JsonObject): EmitterShapePoint? {
        val obj = c.getAsJsonObject("minecraft:emitter_shape_point") ?: return null
        return EmitterShapePoint(
            offset = obj.getExprList("offset"),
            direction = parseDirection(obj)
        )
    }

    private fun parseShapeSphere(c: JsonObject): EmitterShapeSphere? {
        val obj = c.getAsJsonObject("minecraft:emitter_shape_sphere") ?: return null
        return EmitterShapeSphere(
            offset = obj.getExprList("offset"),
            radius = obj.getExpr("radius", "1"),
            surfaceOnly = obj.get("surface_only")?.asBoolean ?: false,
            direction = parseDirection(obj)
        )
    }

    private fun parseShapeBox(c: JsonObject): EmitterShapeBox? {
        val obj = c.getAsJsonObject("minecraft:emitter_shape_box") ?: return null
        return EmitterShapeBox(
            offset = obj.getExprList("offset"),
            halfDimensions = obj.getExprList("half_dimensions"),
            surfaceOnly = obj.get("surface_only")?.asBoolean ?: false,
            direction = parseDirection(obj)
        )
    }

    private fun parseShapeDisc(c: JsonObject): EmitterShapeDisc? {
        val obj = c.getAsJsonObject("minecraft:emitter_shape_disc") ?: return null
        return EmitterShapeDisc(
            offset = obj.getExprList("offset"),
            radius = obj.getExpr("radius", "1"),
            planeNormal = obj.getExprList("plane_normal"),
            surfaceOnly = obj.get("surface_only")?.asBoolean ?: false,
            direction = parseDirection(obj)
        )
    }

    // --- Particle Initial State ---

    private fun parseInitialSpeed(c: JsonObject): ParticleInitialSpeed? {
        val el = c.get("minecraft:particle_initial_speed") ?: return null
        return ParticleInitialSpeed(speed = el.asExpr())
    }

    private fun parseInitialSpin(c: JsonObject): ParticleInitialSpin? {
        val obj = c.getAsJsonObject("minecraft:particle_initial_spin") ?: return null
        return ParticleInitialSpin(
            rotation = obj.getExpr("rotation", "0"),
            rotationRate = obj.getExpr("rotation_rate", "0")
        )
    }

    // --- Particle Lifetime ---

    private fun parseParticleLifetime(c: JsonObject): ParticleLifetimeExpression? {
        val obj = c.getAsJsonObject("minecraft:particle_lifetime_expression") ?: return null
        return ParticleLifetimeExpression(
            maxLifetime = obj.getExpr("max_lifetime", "1"),
            expirationExpression = obj.get("expiration_expression")?.asExpr()
        )
    }

    // --- Particle Motion ---

    private fun parseMotionDynamic(c: JsonObject): ParticleMotionDynamic? {
        val obj = c.getAsJsonObject("minecraft:particle_motion_dynamic") ?: return null
        return ParticleMotionDynamic(
            linearAcceleration = obj.getExprList("linear_acceleration"),
            linearDragCoefficient = obj.getExpr("linear_drag_coefficient", "0"),
            rotationAcceleration = obj.getExpr("rotation_acceleration", "0"),
            rotationDragCoefficient = obj.getExpr("rotation_drag_coefficient", "0")
        )
    }

    private fun parseMotionParametric(c: JsonObject): ParticleMotionParametric? {
        val obj = c.getAsJsonObject("minecraft:particle_motion_parametric") ?: return null
        return ParticleMotionParametric(
            relativePosition = if (obj.has("relative_position")) obj.getExprList("relative_position") else null,
            direction = if (obj.has("direction")) obj.getExprList("direction") else null,
            rotation = obj.get("rotation")?.asExpr()
        )
    }

    // --- Particle Appearance ---

    private fun parseBillboard(c: JsonObject): ParticleAppearanceBillboard? {
        val obj = c.getAsJsonObject("minecraft:particle_appearance_billboard") ?: return null
        val size = obj.getExprList("size", 2)
        val facing = when (obj.get("facing_camera_mode")?.asString?.lowercase()) {
            "rotate_xyz" -> FacingCameraMode.ROTATE_XYZ
            "lookat_xyz" -> FacingCameraMode.LOOKAT_XYZ
            "lookat_y" -> FacingCameraMode.LOOKAT_Y
            "direction_x" -> FacingCameraMode.DIRECTION_X
            "direction_z" -> FacingCameraMode.DIRECTION_Z
            "emitter_transform_xy" -> FacingCameraMode.EMITTER_TRANSFORM_XY
            "emitter_transform_xz" -> FacingCameraMode.EMITTER_TRANSFORM_XZ
            "emitter_transform_yz" -> FacingCameraMode.EMITTER_TRANSFORM_YZ
            else -> FacingCameraMode.ROTATE_XYZ
        }
        val uv = parseUV(obj.getAsJsonObject("uv"))
        return ParticleAppearanceBillboard(size = size, facingCameraMode = facing, uv = uv)
    }

    private fun parseUV(uvObj: JsonObject?): ParticleUV {
        if (uvObj == null) return ParticleUV.Static()

        // Check for flipbook
        val flipbook = uvObj.getAsJsonObject("flipbook")
        if (flipbook != null) {
            val texW = uvObj.get("texture_width")?.asInt ?: 128
            val texH = uvObj.get("texture_height")?.asInt ?: 128
            return ParticleUV.Flipbook(
                baseUV = flipbook.getExprList("base_UV", 2),
                sizeUV = flipbook.getAsJsonArray("size_UV")?.map { it.asFloat } ?: listOf(1f, 1f),
                stepUV = flipbook.getAsJsonArray("step_UV")?.map { it.asFloat } ?: listOf(1f, 0f),
                framesPerSecond = flipbook.get("frames_per_second")?.asFloat ?: 8f,
                maxFrame = flipbook.getExpr("max_frame", "1"),
                stretchToLifetime = flipbook.get("stretch_to_lifetime")?.asBoolean ?: false,
                loop = flipbook.get("loop")?.asBoolean ?: false,
                textureWidth = texW,
                textureHeight = texH
            )
        }

        // Static UV
        val texW = uvObj.get("texture_width")?.asInt ?: 1
        val texH = uvObj.get("texture_height")?.asInt ?: 1
        val uv = if (uvObj.has("uv")) uvObj.getExprList("uv", 2) else listOf("0", "0")
        val uvSize = if (uvObj.has("uv_size")) uvObj.getExprList("uv_size", 2) else listOf("$texW", "$texH")
        return ParticleUV.Static(textureWidth = texW, textureHeight = texH, uv = uv, uvSize = uvSize)
    }

    private fun parseTinting(c: JsonObject): ParticleAppearanceTinting? {
        val obj = c.getAsJsonObject("minecraft:particle_appearance_tinting") ?: return null

        val colorEl = obj.get("color") ?: return null

        // Check for gradient (color is an object with "gradient" key)
        if (colorEl.isJsonObject) {
            val colorObj = colorEl.asJsonObject
            if (colorObj.has("gradient")) {
                val gradientMap = mutableMapOf<Float, List<Float>>()
                val gradientObj = colorObj.get("gradient")
                if (gradientObj.isJsonObject) {
                    for ((key, value) in gradientObj.asJsonObject.entrySet()) {
                        val pos = key.toFloatOrNull() ?: continue
                        gradientMap[pos] = parseColorValue(value)
                    }
                } else if (gradientObj.isJsonArray) {
                    // Gradient as array: evenly spaced colors
                    val arr = gradientObj.asJsonArray
                    for (i in 0 until arr.size()) {
                        val pos = if (arr.size() > 1) i.toFloat() / (arr.size() - 1) else 0f
                        gradientMap[pos] = parseColorValue(arr[i])
                    }
                }
                val interpolant = colorObj.getExpr("interpolant",
                    "variable.particle_age / variable.particle_lifetime")
                return ParticleAppearanceTinting.GradientColor(
                    interpolant = interpolant,
                    gradient = gradientMap
                )
            }
        }

        // Static color (array of expressions like ["1", "0", "0", "1"])
        if (colorEl.isJsonArray) {
            return ParticleAppearanceTinting.StaticColor(
                color = colorEl.asJsonArray.map { it.asExpr() }
            )
        }
        // Single color string (hex)
        if (colorEl.isJsonPrimitive) {
            val parsed = parseColorValue(colorEl)
            return ParticleAppearanceTinting.StaticColor(
                color = parsed.map { it.toString() }
            )
        }
        return ParticleAppearanceTinting.StaticColor(color = listOf("1", "1", "1", "1"))
    }

    private fun parseColorValue(el: JsonElement): List<Float> {
        if (el.isJsonArray) return el.asJsonArray.map { it.asFloat }
        if (el.isJsonPrimitive && el.asString.startsWith("#")) {
            val hex = el.asString.removePrefix("#")
            val r = hex.substring(0, 2).toInt(16) / 255f
            val g = hex.substring(2, 4).toInt(16) / 255f
            val b = hex.substring(4, 6).toInt(16) / 255f
            val a = if (hex.length >= 8) hex.substring(6, 8).toInt(16) / 255f else 1f
            return listOf(r, g, b, a)
        }
        return listOf(1f, 1f, 1f, 1f)
    }

    // --- Curves ---

    private fun parseCurves(curvesObj: JsonObject?): Map<String, CurveDefinition> {
        if (curvesObj == null) return emptyMap()
        val result = mutableMapOf<String, CurveDefinition>()
        for ((name, el) in curvesObj.entrySet()) {
            if (!el.isJsonObject) continue
            val obj = el.asJsonObject
            val type = when (obj.get("type")?.asString?.lowercase()) {
                "linear" -> CurveType.LINEAR
                "bezier" -> CurveType.BEZIER
                "catmull_rom" -> CurveType.CATMULL_ROM
                "bezier_chain" -> CurveType.BEZIER_CHAIN
                else -> CurveType.LINEAR
            }
            val nodes = obj.getAsJsonArray("nodes")?.map { it.asFloat } ?: emptyList()
            result[name] = CurveDefinition(
                type = type,
                input = obj.getExpr("input", "variable.particle_age / variable.particle_lifetime"),
                horizontalRange = obj.getExpr("horizontal_range", "1"),
                nodes = nodes
            )
        }
        return result
    }
}
