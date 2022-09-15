import com.soywiz.kds.iterators.*
import kotlinx.coroutines.*
import kotlin.math.*

class StateCalculator(
    private val states: ParticleStates,
    private val weights: ForceWeights,
    private val updateDelayMs: Long,
    var gravityWell: Double,
) {

    fun calculate(scope: CoroutineScope, dispatcher: CoroutineDispatcher) {
        states.getTypes().forEach { fromType ->
            states.get(fromType).forEach { state ->
                scope.launch(dispatcher) {
                    while (true) {
                        states.getTypes().forEach { toType ->
                            state.applyRule(
                                myMass = states.getMass(fromType),
                                otherMass = states.getMass(toType),
                                otherParticles = states.get(toType),
                                gravity = weights.getWeight(fromType, toType),
                                gravityWell = gravityWell
                            )
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
    private val massMap = mutableMapOf<ParticleType, Float>()

    fun get(type: ParticleType): List<ParticleState> = stateMap[type]!!

    fun getTypes(): Set<ParticleType> = stateMap.keys

    fun getMass(type: ParticleType): Float = massMap[type]!!

    fun setMass(mass: Float) { massMap.keys.forEach { massMap[it] = mass } }

    fun randomizeMass() {
        println("--------------------------------------------------------------------------------")
        massMap.keys.forEach {
            val mass = RandomUtil.randomMass()
            massMap[it] = mass
            println("$it mass: $mass")
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
                    typeStates.add(ParticleState(posX = RandomUtil.randomXpos(), posY = RandomUtil.randomYpos()))
                }
                states.stateMap[type] = typeStates
                states.massMap[type] = MainConstants.DEFAULT_PARTICLE_MASS
            }

            return states
        }
    }
}

data class ParticleState(var posX: Float, var posY: Float) {

    var velX = 0.0f
    var velY = 0.0f
    var scaleX = MainConstants.DEFAULT_PARTICLE_SCALE
    var scaleY = MainConstants.DEFAULT_PARTICLE_SCALE
    var anchorX = 0f
    var anchorY = 0f

    fun applyRule(
        myMass: Float,
        otherMass: Float,
        otherParticles: List<ParticleState>,
        gravity: Float,
        gravityWell: Double)
    {
        val (forceX, forceY) = Rules.calculateForce(this, myMass, otherMass, otherParticles, gravity, gravityWell)
        Rules.calculateXPosXVel(this, forceX).let {
            posX = it.first
            velX = it.second
        }
        Rules.calculateYPosYVel(this, forceY).let {
            posY = it.first
            velY = it.second
        }

        scaleX = Rules.calculateScale(velY)
        scaleY = Rules.calculateScale(velX)
        anchorX = (MainConstants.BITMAP_SIZE * scaleX) / 2f
        anchorY = (MainConstants.BITMAP_SIZE * scaleY) / 2f
    }
}

class Rules {

    companion object {
        private const val MAX_SCALE_VEL = 0.6f
        private const val MAX_SCALE = 0.27f
        private const val MIN_SCALE = 0.09f
        private const val MIN_X = MainConstants.WALL_BUFFER + MainConstants.WALL_THICKNESS
        private const val MAX_X = MainConstants.WIDTH - MainConstants.WALL_BUFFER - MainConstants.WALL_THICKNESS
        private const val MIN_Y = MainConstants.WALL_BUFFER + MainConstants.WALL_THICKNESS
        private const val MAX_Y = MainConstants.HEIGHT - MainConstants.WALL_BUFFER - MainConstants.WALL_THICKNESS
        private const val FORCE_SCALE = 0.3f

        fun calculateForce(
            myParticle: ParticleState,
            myMass: Float,
            otherMass: Float,
            otherParticles: List<ParticleState>,
            gravity: Float,
            gravityWell: Double
        ): Pair<Float, Float> {
            var forceX = 0.0f
            var forceY = 0.0f
            otherParticles.fastForEach { otherParticle ->
                val disX = myParticle.posX - otherParticle.posX
                val disY = myParticle.posY - otherParticle.posY
                val disTotal = sqrt(disX*disX + disY*disY)
                if (disTotal > 0 && disTotal < gravityWell) {
                    val force = (gravity * myMass * otherMass) / (disTotal * disTotal)
                    forceX += (force * disX)
                    forceY += (force * disY)
                }
            }

            return forceX to forceY
        }

        fun calculateXPosXVel(myParticle: ParticleState, forceX: Float): Pair<Float, Float> {
            var newVelX = (myParticle.velX + forceX) * FORCE_SCALE
            var newX = myParticle.posX + newVelX

            if (newX < MIN_X + myParticle.anchorX*2) {
                newX = MIN_X + myParticle.anchorX*2
                newVelX *= -1
            } else if (newX > MAX_X) {
                newX = MAX_X
                newVelX *= -1
            }

            return newX to newVelX
        }

        fun calculateYPosYVel(myParticle: ParticleState, forceY: Float): Pair<Float, Float> {
            var newVelY = (myParticle.velY + forceY) * FORCE_SCALE
            var newY = myParticle.posY + newVelY

            if (newY < MIN_Y + myParticle.anchorY*2) {
                newY = MIN_Y + myParticle.anchorY*2
                newVelY *= -1
            } else if (newY > MAX_Y) {
                newY = MAX_Y
                newVelY *= -1
            }

            return newY to newVelY
        }

        fun calculateScale(value: Float): Float {
            val vel = if (abs(value) > MAX_SCALE_VEL) MAX_SCALE_VEL else abs(value)

            return MAX_SCALE - (vel/MAX_SCALE_VEL) * (MAX_SCALE - MIN_SCALE)
        }
    }
}
