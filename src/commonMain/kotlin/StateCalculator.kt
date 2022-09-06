import com.soywiz.kds.iterators.*
import kotlinx.coroutines.*
import kotlin.math.*

class StateCalculator(
    private val yellowParticles: List<ParticleState>,
    private val orangeParticles: List<ParticleState>,
    private val whiteParticles: List<ParticleState>,
    private val redParticles: List<ParticleState>,
    private val purpleParticles: List<ParticleState>,
    private val weights: ForceWeights,
    private val updateDelayMs: Long,
    var gravityWell: Double,
) {

    private val allParticles = listOf(yellowParticles, orangeParticles, whiteParticles, redParticles, purpleParticles).flatten()

    fun calculate(scope: CoroutineScope, dispatcher: CoroutineDispatcher) {
        yellowParticles.forEach { yellowParticle ->
            scope.launch(dispatcher) {
                while (true) {
                    yellowParticle.applyRule(whiteParticles, weights.yellowWhite, gravityWell)
                    yellowParticle.applyRule(orangeParticles, weights.yellowOrange, gravityWell)
                    yellowParticle.applyRule(yellowParticles, weights.yellowYellow, gravityWell)
                    yellowParticle.applyRule(redParticles, weights.yellowRed, gravityWell)
                    yellowParticle.applyRule(purpleParticles, weights.yellowPurple, gravityWell)
                    delay(updateDelayMs)
                }
            }
        }
        orangeParticles.forEach { orangeParticle ->
            scope.launch(dispatcher) {
                while (true) {
                    orangeParticle.applyRule(whiteParticles, weights.orangeWhite, gravityWell)
                    orangeParticle.applyRule(orangeParticles, weights.orangeOrange, gravityWell)
                    orangeParticle.applyRule(yellowParticles, weights.orangeYellow, gravityWell)
                    orangeParticle.applyRule(redParticles, weights.orangeRed, gravityWell)
                    orangeParticle.applyRule(purpleParticles, weights.orangePurple, gravityWell)
                    delay(updateDelayMs)
                }
            }
        }
        whiteParticles.forEach { whiteParticle ->
            scope.launch(dispatcher) {
                while (true) {
                    whiteParticle.applyRule(whiteParticles, weights.whiteWhite, gravityWell)
                    whiteParticle.applyRule(orangeParticles, weights.whiteOrange, gravityWell)
                    whiteParticle.applyRule(yellowParticles, weights.whiteYellow, gravityWell)
                    whiteParticle.applyRule(redParticles, weights.whiteRed, gravityWell)
                    whiteParticle.applyRule(purpleParticles, weights.whitePurple, gravityWell)
                    delay(updateDelayMs)
                }
            }
        }
        redParticles.forEach { redParticle ->
            scope.launch(dispatcher) {
                while (true) {
                    redParticle.applyRule(whiteParticles, weights.redWhite, gravityWell)
                    redParticle.applyRule(orangeParticles, weights.redOrange, gravityWell)
                    redParticle.applyRule(yellowParticles, weights.redYellow, gravityWell)
                    redParticle.applyRule(redParticles, weights.redRed, gravityWell)
                    redParticle.applyRule(purpleParticles, weights.redPurple, gravityWell)
                    delay(updateDelayMs)
                }
            }
        }
        purpleParticles.forEach { purpleParticle ->
            scope.launch(dispatcher) {
                while (true) {
                    purpleParticle.applyRule(whiteParticles, weights.purpleWhite, gravityWell)
                    purpleParticle.applyRule(orangeParticles, weights.purpleOrange, gravityWell)
                    purpleParticle.applyRule(yellowParticles, weights.purpleYellow, gravityWell)
                    purpleParticle.applyRule(redParticles, weights.purpleRed, gravityWell)
                    purpleParticle.applyRule(purpleParticles, weights.purplePurple, gravityWell)
                    delay(updateDelayMs)
                }
            }
        }
    }

    fun setMass(mass: Float) { allParticles.fastForEach { it.mass = mass } }

    fun randomizeMass() {
        val whiteMass = RandomUtil.randomMass()
        whiteParticles.fastForEach { it.mass = whiteMass }
        val yellowMass = RandomUtil.randomMass()
        yellowParticles.fastForEach { it.mass = yellowMass }
        val orangeMass = RandomUtil.randomMass()
        orangeParticles.fastForEach { it.mass = orangeMass }
        val redMass = RandomUtil.randomMass()
        redParticles.fastForEach { it.mass = redMass }
        val purpleMass = RandomUtil.randomMass()
        purpleParticles.fastForEach { it.mass = purpleMass }
    }

    fun resetPosition() {
        allParticles.fastForEach {
            it.posX = RandomUtil.randomXpos()
            it.posY = RandomUtil.randomYpos()
            it.velX = 0f
            it.velY = 0f
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

    fun applyRule(otherAtoms: List<ParticleState>, gravity: Float, gravityWell: Double) {
        val (forceX, forceY) = calculateForce(otherAtoms, gravity, gravityWell)
        val (newX, newVelX) = calculateXposAndVel(forceX)
        val (newY, newVelY) = calculateYposAndVel(forceY)

        posX = newX
        posY = newY
        velX = newVelX
        velY = newVelY
        scaleX = normalizeParticleScale(abs(newVelY))
        scaleY = normalizeParticleScale(abs(newVelX))
        anchorX = (MainConstants.BITMAP_SIZE * scaleX) / 2f
        anchorY = (MainConstants.BITMAP_SIZE * scaleY) / 2f
    }

    private fun calculateForce(otherAtoms: List<ParticleState>, gravity: Float, gravityWell: Double): Pair<Float, Float> {
        var forceX = 0.0f
        var forceY = 0.0f
        otherAtoms.fastForEach { otherAtom ->
            val disX = posX - otherAtom.posX
            val disY = posY - otherAtom.posY
            val disTotal = sqrt(disX*disX + disY*disY)
            if (disTotal > 0 && disTotal < gravityWell) {
                val force = (gravity * mass * otherAtom.mass) / (disTotal * disTotal)
                forceX += (force * disX)
                forceY += (force * disY)
            }
        }

        return forceX to forceY
    }

    private fun calculateXposAndVel(forceX: Float): Pair<Float, Float> {
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

    private fun calculateYposAndVel(forceY: Float): Pair<Float, Float> {
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
