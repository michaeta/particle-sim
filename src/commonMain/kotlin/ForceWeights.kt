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
    }

    fun randomOffset() {
        weightsMap.values.forEach { toMap -> toMap.keys.forEach { toMap[it] = toMap[it]!! + RandomUtil.smallRandomWeight() } }
    }

    companion object {
        fun buildDefault(): ForceWeights {
            val weights = ForceWeights()
            ParticleType.values().forEach { fromType ->
                ParticleType.values().forEach { toType ->
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
    PURPLE(Colors.MEDIUMPURPLE)
}
