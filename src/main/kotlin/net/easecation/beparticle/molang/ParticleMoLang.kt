package net.easecation.beparticle.molang

import team.unnamed.mocha.parser.MolangParser
import team.unnamed.mocha.parser.ast.Expression
import team.unnamed.mocha.runtime.ExpressionInterpreter
import team.unnamed.mocha.runtime.Scope
import team.unnamed.mocha.runtime.value.MutableObjectBinding
import team.unnamed.mocha.runtime.value.NumberValue
import team.unnamed.mocha.runtime.value.Value
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap

/**
 * MoLang expression evaluator for particle system.
 * Uses team.unnamed:mocha parser + interpreter directly (same pattern as BedrockMotion).
 * Includes parse caching for performance.
 */
@Suppress("UnstableApiUsage")
object ParticleMoLang {
    private val PARSE_CACHE = ConcurrentHashMap<String, List<Expression>>()
    private const val MAX_CACHE_SIZE = 4096

    // Thread-local scope to avoid allocation per eval
    private val EVAL_SCOPE = ThreadLocal.withInitial { LayeredScope(Scope.create()) }

    /**
     * Evaluate a MoLang expression string with the given variable bindings.
     * Returns the float result.
     */
    fun eval(expression: String, variables: Map<String, Float> = emptyMap()): Float {
        if (expression.isBlank()) return 0f

        // Fast path: numeric literals
        val first = expression[0]
        if (first in '0'..'9' || first == '-' || first == '.') {
            try {
                return expression.toDouble().toFloat()
            } catch (_: NumberFormatException) {
                // Not a pure number, fall through
            }
        }

        return try {
            val expressions = parse(expression)
            val scope = buildScope(variables)
            val result = evalExpressions(scope, expressions).getAsNumber().toFloat()
            if (result.isNaN() || result.isInfinite()) 0f else result
        } catch (e: Exception) {
            expression.toFloatOrNull() ?: 0f
        }
    }

    /**
     * Parse and cache a MoLang expression.
     */
    fun parse(expression: String): List<Expression> {
        PARSE_CACHE[expression]?.let { return it }

        val parsed = StringReader(expression).use { reader ->
            MolangParser.parser(reader).parseAll()
        }
        if (PARSE_CACHE.size < MAX_CACHE_SIZE) {
            PARSE_CACHE[expression] = parsed
        }
        return parsed
    }

    /**
     * Evaluate pre-parsed expressions with a scope.
     */
    private fun evalExpressions(scope: Scope, expressions: List<Expression>): Value {
        val localScope = EVAL_SCOPE.get()
        localScope.reset(scope)
        val tempBinding = MutableObjectBinding()
        localScope.set("temp", tempBinding)
        localScope.set("t", tempBinding)
        localScope.readOnly(true)

        val evaluator = ExpressionInterpreter<Void>(null, localScope)
        evaluator.warnOnReflectiveFunctionUsage(false)

        var lastResult: Value = NumberValue.zero()
        for (expr in expressions) {
            lastResult = expr.visit(evaluator)
            val returnValue = evaluator.popReturnValue()
            if (returnValue != null) {
                lastResult = returnValue
                break
            }
        }
        return lastResult
    }

    /**
     * Build a Scope from a variable map.
     * Variables are set under the "variable" binding (accessible as variable.xxx).
     */
    private fun buildScope(variables: Map<String, Float>): Scope {
        val scope = Scope.create()
        if (variables.isNotEmpty()) {
            val variableBinding = MutableObjectBinding()
            for ((key, value) in variables) {
                variableBinding.set(key, NumberValue.of(value.toDouble()))
            }
            scope.set("variable", variableBinding)
            scope.set("v", variableBinding)
        }
        return scope
    }

    /**
     * Create a variable map with emitter-level variables.
     */
    fun emitterVars(
        emitterAge: Float,
        emitterLifetime: Float,
        emitterRandom1: Float,
        emitterRandom2: Float,
        emitterRandom3: Float,
        emitterRandom4: Float
    ): MutableMap<String, Float> = mutableMapOf(
        "emitter_age" to emitterAge,
        "emitter_lifetime" to emitterLifetime,
        "emitter_random_1" to emitterRandom1,
        "emitter_random_2" to emitterRandom2,
        "emitter_random_3" to emitterRandom3,
        "emitter_random_4" to emitterRandom4
    )

    /**
     * Add particle-level variables to an existing map.
     */
    fun addParticleVars(
        vars: MutableMap<String, Float>,
        particleAge: Float,
        particleLifetime: Float,
        particleRandom1: Float,
        particleRandom2: Float,
        particleRandom3: Float,
        particleRandom4: Float
    ): MutableMap<String, Float> {
        vars["particle_age"] = particleAge
        vars["particle_lifetime"] = particleLifetime
        vars["particle_random_1"] = particleRandom1
        vars["particle_random_2"] = particleRandom2
        vars["particle_random_3"] = particleRandom3
        vars["particle_random_4"] = particleRandom4
        return vars
    }

    /**
     * Clear the parse cache.
     */
    fun clearCache() {
        PARSE_CACHE.clear()
    }
}
