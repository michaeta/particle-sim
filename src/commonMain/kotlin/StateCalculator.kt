import com.soywiz.kds.iterators.*
import kotlinx.coroutines.*
import kotlin.math.*

class StateCalculator(
    private val particleStates: ParticleStates,
    private val weights: ForceWeights,
    private val updateDelayMs: Long,
    var gravityWell: Double,
) {

    fun calculate(scope: CoroutineScope, dispatcher: CoroutineDispatcher) {
        particleStates.getTypes().forEach { fromType ->
            particleStates.get(fromType).forEach { state ->
                scope.launch(dispatcher) {
                    while (true) {
                        particleStates.getTypes().forEach { toType ->
                            state.applyRule(particleStates.get(toType), weights.getWeight(fromType, toType), gravityWell)
                        }
                        delay(updateDelayMs)
                    }
                }
            }
        }
    }
}

class ParticleStates {

    private val stateMap = mutableMapOf<ParticleType, MutableList<ParticleState>>()

    fun get(type: ParticleType): List<ParticleState> = stateMap[type]!!

    fun getTypes(): Set<ParticleType> = stateMap.keys

    fun setMass(mass: Float) { stateMap.values.flatten().fastForEach { it.mass = mass } }

    fun randomizeMass() {
        getTypes().forEach { type ->
            val randomMass = RandomUtil.randomMass()
            get(type).fastForEach { it.mass = randomMass }
        }
    }

    fun resetPosition() {
        stateMap.values.flatten().fastForEach {
            it.posX = RandomUtil.randomXpos()
            it.posY = RandomUtil.randomYpos()
            it.velX = 0f
            it.velY = 0f
        }
    }

    companion object {
        fun buildDefault(countPerColor: Int): ParticleStates = buildForTypes(ParticleType.values().toSet(), countPerColor)

        fun buildForTypes(types: Set<ParticleType>, countPerColor: Int): ParticleStates {
            val states = ParticleStates()
            types.forEach { type ->
                val typeStates = mutableListOf<ParticleState>()
                for (i in 1 .. countPerColor) {
                    typeStates.add(
                        ParticleState(
                            mass = MainConstants.DEFAULT_PARTICLE_MASS,
                            posX = RandomUtil.randomXpos(),
                            posY = RandomUtil.randomYpos()
                        )
                    )
                }
                states.stateMap[type] = typeStates
            }

            return states
        }
    }
}

data class ParticleState(var mass: Float, var posX: Float, var posY: Float) {

    object Constants {
        const val MAX_SCALE_VEL = 0.6f
        const val MAX_SCALE = 0.27f
        const val MIN_SCALE = 0.09f
        const val X_MIN = MainConstants.WALL_BUFFER + MainConstants.WALL_THICKNESS
        const val X_MAX = MainConstants.WIDTH - MainConstants.WALL_BUFFER - MainConstants.WALL_THICKNESS
        const val Y_MIN = MainConstants.WALL_BUFFER + MainConstants.WALL_THICKNESS
        const val Y_MAX = MainConstants.HEIGHT - MainConstants.WALL_BUFFER - MainConstants.WALL_THICKNESS
        const val FORCE_SCALE = 0.4f
    }

    var velX = 0.0f
    var velY = 0.0f
    var scaleX = MainConstants.DEFAULT_PARTICLE_SCALE
    var scaleY = MainConstants.DEFAULT_PARTICLE_SCALE
    var anchorX = 0f
    var anchorY = 0f

    fun applyRule(otherParticles: List<ParticleState>, gravity: Float, gravityWell: Double) {
        val (forceX, forceY) = calculateForce(otherParticles, gravity, gravityWell)
        val (newX, newVelX) = calculateXPosXVel(forceX)
        val (newY, newVelY) = calculateYPosYVel(forceY)

        posX = newX
        posY = newY
        velX = newVelX
        velY = newVelY
        scaleX = normalizeParticleScale(abs(newVelY))
        scaleY = normalizeParticleScale(abs(newVelX))
        anchorX = (MainConstants.BITMAP_SIZE * scaleX) / 2f
        anchorY = (MainConstants.BITMAP_SIZE * scaleY) / 2f
    }

    private fun calculateForce(
        otherParticles: List<ParticleState>,
        gravity: Float,
        gravityWell: Double
    ): Pair<Float, Float> {
        var forceX = 0.0f
        var forceY = 0.0f
        otherParticles.fastForEach { otherParticle ->
            val disX = posX - otherParticle.posX
            val disY = posY - otherParticle.posY
            val disTotal = sqrt(disX*disX + disY*disY)
            if (disTotal > 0 && disTotal < gravityWell) {
                val force = (gravity * mass * otherParticle.mass) / (disTotal * disTotal)
                forceX += (force * disX)
                forceY += (force * disY)
            }
        }

        return forceX to forceY
    }

    private fun calculateXPosXVel(forceX: Float): Pair<Float, Float> {
        var newVelX = (velX + forceX) * Constants.FORCE_SCALE
        var newX = posX + newVelX

        if (newX < Constants.X_MIN + anchorX*2) {
            newX = Constants.X_MIN + anchorX*2
            newVelX *= -1
        } else if (newX > Constants.X_MAX) {
            newX = Constants.X_MAX
            newVelX *= -1
        }

        return newX to newVelX
    }

    private fun calculateYPosYVel(forceY: Float): Pair<Float, Float> {
        var newVelY = (velY + forceY) * Constants.FORCE_SCALE
        var newY = posY + newVelY

        if (newY < Constants.Y_MIN + anchorY*2) {
            newY = Constants.Y_MIN + anchorY*2
            newVelY *= -1
        } else if (newY > Constants.Y_MAX) {
            newY = Constants.Y_MAX
            newVelY *= -1
        }

        return newY to newVelY
    }

    private fun normalizeParticleScale(value: Float): Float {
        val vel = if (value > Constants.MAX_SCALE_VEL) Constants.MAX_SCALE_VEL else value

        return Constants.MAX_SCALE - (vel/Constants.MAX_SCALE_VEL) * (Constants.MAX_SCALE - Constants.MIN_SCALE)
    }
}
