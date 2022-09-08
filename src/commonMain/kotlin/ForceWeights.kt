import com.soywiz.korim.color.*

class ForceWeights {

    private val weightsMap = mutableMapOf<ParticleType, MutableMap<ParticleType, Float>>()


    fun getWeight(from: ParticleType, to: ParticleType): Float = weightsMap[from]!![to]!!

    fun putWeight(from: ParticleType, to: ParticleType, weight: Float) {
        if (!weightsMap.containsKey(from)) { weightsMap[from] = mutableMapOf() }
        weightsMap[from]!![to] = weight
    }

    fun randomize() {
        weightsMap.values.forEach { toMap -> toMap.keys.forEach { toMap[it] = RandomUtil.randomWeight() } }
        printWeights()
    }

    fun randomOffset() {
        weightsMap.values.forEach { toMap -> toMap.keys.forEach { toMap[it] = toMap[it]!! + RandomUtil.smallRandomWeight() } }
        printWeights()
    }

    private fun printWeights() {
        println("--------------------------------------------------------------------------------")
        weightsMap.keys.forEach { fromType ->
            weightsMap[fromType]!!.keys.forEach { toType ->
                println("$fromType to $toType weight: ${weightsMap[fromType]!![toType]}")
            }
        }
    }

    companion object {
        fun buildDefault(): ForceWeights = buildForTypes(ParticleType.values().toSet())

        fun buildForTypes(types: Set<ParticleType>): ForceWeights {
            val weights = ForceWeights()
            types.forEach { fromType ->
                types.forEach { toType ->
                    weights.putWeight(fromType, toType, RandomUtil.randomWeight())
                }
            }

            return weights
        }
    }
}

enum class ParticleType(val color: RGBA) {
    YELLOW(Colors.YELLOW),
    ORANGE(Colors.ORANGE),
    WHITE(Colors.WHITE),
    RED(Colors.RED),
    PURPLE(Colors.MEDIUMPURPLE),
    GREEN(Colors.GREEN),
    PINK(Colors.HOTPINK),
    GRAY(Colors.DIMGRAY)
}
