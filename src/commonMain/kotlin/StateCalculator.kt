import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
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

class FastStateCalculator(
    private val particleStates: FastParticleStates,
    private val weights: ForceWeights,
    private val updateDelayMs: Long,
    var gravityWell: Double,
) {

    fun calculate(scope: CoroutineScope, dispatcher: CoroutineDispatcher) {
        particleStates.getTypes().forEach { fromType ->
            val buffer = particleStates.get(fromType)
            for (i in 0 until particleStates.countPerColor) {
                scope.launch(dispatcher) {
                    while (true) {
                        particleStates.getTypes().forEach { toType ->
                            particleStates.applyRule(i, buffer, particleStates.get(toType), weights.getWeight(fromType, toType), gravityWell)
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

class FastParticleStates private constructor(val countPerColor: Int) {

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

    private val bufferMap = mutableMapOf<ParticleType, Float32Buffer>()

    fun get(type: ParticleType): Float32Buffer = bufferMap[type]!!

    fun getTypes(): Set<ParticleType> = bufferMap.keys

    fun setMass(mass: Float) {
        bufferMap.values.forEach { buffer ->
            for (i in 0 until countPerColor) { buffer[i * 9 + 0] = mass }
        }
    }

    fun randomizeMass() {
        bufferMap.values.forEach { buffer ->
            val mass = RandomUtil.randomMass()
            for (i in 0 until countPerColor) { buffer[i * 9 + 0] = mass }
        }
    }

    fun resetPosition() {
        bufferMap.values.forEach { buffer ->
            for (i in 0 until countPerColor) {
                buffer[i * 9 + 1] = RandomUtil.randomXpos()
                buffer[i * 9 + 2] = RandomUtil.randomYpos()
                buffer[i * 9 + 3] = 0f
                buffer[i * 9 + 4] = 0f
            }
        }
    }

    fun applyRule(index: Int, myBuffer: Float32Buffer, otherBuffer: Float32Buffer, gravity: Float, gravityWell: Double) {
        val (forceX, forceY) = calculateForce(index, myBuffer, otherBuffer, gravity, gravityWell)
        val (newX, newVelX) = calculateXPosXVel(index, myBuffer, forceX)
        val (newY, newVelY) = calculateYPosYVel(index, myBuffer, forceY)

        myBuffer[index * 9 + 1] = newX
        myBuffer[index * 9 + 2] = newY
        myBuffer[index * 9 + 3] = newVelX
        myBuffer[index * 9 + 4] = newVelY
        myBuffer[index * 9 + 5] = normalizeParticleScale(abs(newVelY))
        myBuffer[index * 9 + 6] = normalizeParticleScale(abs(newVelX))
        myBuffer[index * 9 + 7] = (MainConstants.BITMAP_SIZE * myBuffer[index * 9 + 5]) / 2f
        myBuffer[index * 9 + 8] = (MainConstants.BITMAP_SIZE * myBuffer[index * 9 + 6]) / 2f
    }

    private fun calculateForce(
        index: Int,
        myBuffer: Float32Buffer,
        otherBuffer: Float32Buffer,
        gravity: Float,
        gravityWell: Double
    ): Pair<Float, Float> {
        var forceX = 0.0f
        var forceY = 0.0f
        for (i in 0 until countPerColor) {
            val disX = myBuffer[index * 9 + 1] - otherBuffer[i * 9 + 1]
            val disY = myBuffer[index * 9 + 2] - otherBuffer[i * 9 + 2]
            val disTotal = sqrt(disX*disX + disY*disY)
            if (disTotal > 0 && disTotal < gravityWell) {
                val force = (gravity * myBuffer[index * 9 + 0] * otherBuffer[i * 9 + 0]) / (disTotal * disTotal)
                forceX += (force * disX)
                forceY += (force * disY)
            }
        }

        return forceX to forceY
    }

    private fun calculateXPosXVel(index: Int, myBuffer: Float32Buffer, forceX: Float): Pair<Float, Float> {
        var newVelX = (myBuffer[index * 9 + 3] + forceX) * Constants.FORCE_SCALE
        var newX = myBuffer[index * 9 + 1] + newVelX

        if (newX < Constants.X_MIN + myBuffer[index * 9 + 7]*2) {
            newX = Constants.X_MIN + myBuffer[index * 9 + 7]*2
            newVelX *= -1
        } else if (newX > Constants.X_MAX) {
            newX = Constants.X_MAX
            newVelX *= -1
        }

        return newX to newVelX
    }

    private fun calculateYPosYVel(index: Int, myBuffer: Float32Buffer, forceY: Float): Pair<Float, Float> {
        var newVelY = (myBuffer[index * 9 + 4] + forceY) * Constants.FORCE_SCALE
        var newY = myBuffer[index * 9 + 2] + newVelY

        if (newY < Constants.Y_MIN + myBuffer[index * 9 + 8]*2) {
            newY = Constants.Y_MIN + myBuffer[index * 9 + 8]*2
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

    companion object {
        fun buildDefault(countPerColor: Int): FastParticleStates = buildForTypes(ParticleType.values().toSet(), countPerColor)

        fun buildForTypes(types: Set<ParticleType>, countPerColor: Int): FastParticleStates {
            val states = FastParticleStates(countPerColor)
            types.forEach { type ->
                val buffer = FBuffer(countPerColor * 9 * Float.SIZE_BYTES).f32
                for (i in 0 until countPerColor) {
                    buffer[i * 9 + 0] = MainConstants.DEFAULT_PARTICLE_MASS
                    buffer[i * 9 + 1] = RandomUtil.randomXpos()
                    buffer[i * 9 + 2] = RandomUtil.randomYpos()
                    buffer[i * 9 + 3] = 0f
                    buffer[i * 9 + 4] = 0f
                    buffer[i * 9 + 5] = MainConstants.DEFAULT_PARTICLE_SCALE
                    buffer[i * 9 + 6] = MainConstants.DEFAULT_PARTICLE_SCALE
                    buffer[i * 9 + 7] = 0f
                    buffer[i * 9 + 8] = 0f
                }
                states.bufferMap[type] = buffer
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
