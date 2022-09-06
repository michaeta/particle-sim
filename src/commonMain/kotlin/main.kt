import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.input.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.fast.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.concurrent.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import kotlin.collections.List
import kotlin.collections.forEach
import kotlin.collections.mutableListOf
import kotlin.collections.set

object MainConstants {
    const val WIDTH = 1600
    const val HEIGHT = 900
    const val BITMAP_SIZE = 32
    const val WALL_BUFFER = 1
    const val WALL_THICKNESS = 2f
    const val PARTICLE_COUNT_PER_COLOR = 1000
    const val DEFAULT_PARTICLE_SCALE = 0.125f
    const val DEFAULT_PARTICLE_MASS = 2f
    const val DEFAULT_GRAVITY_WELL = 210.0
    const val THREAD_COUNT = 16
    val POS_UPDATE_DELAY_MS = Frequency(40.0).timeSpan.milliseconds.toLong()
    val GUI_UPDATE_FREQ = Frequency(60.0)
}

suspend fun main() = Korge(
    width = MainConstants.WIDTH,
    height = MainConstants.HEIGHT,
    bgcolor = Colors["#01090e"],
    batchMaxQuads = 2048)
{
    val yellowStates = createParticleStates()
    val orangeStates = createParticleStates()
    val whiteStates = createParticleStates()
    val redStates = createParticleStates()
    val purpleStates = createParticleStates()
    val weights = ForceWeights()
    val stateCalculator = StateCalculator(yellowStates, orangeStates, whiteStates, redStates, purpleStates, weights,
        MainConstants.POS_UPDATE_DELAY_MS, MainConstants.DEFAULT_GRAVITY_WELL)

    drawBorder(MainConstants.WALL_THICKNESS.toInt())
    openPropertiesWindow(weights, stateCalculator)

    val dispatcher = Dispatchers.createFixedThreadDispatcher("particle", MainConstants.THREAD_COUNT)
    val particleBitmap = resourcesVfs["whiteAtom.png"].readBitmap(format = PNG)
    val particleTexture = particleBitmap.sliceWithSize(0, 0, MainConstants.BITMAP_SIZE, MainConstants.BITMAP_SIZE)
    val container = FSprites(MainConstants.PARTICLE_COUNT_PER_COLOR * 5)
    val spriteParticleStateMap = mutableMapOf<Int, ParticleState>()

    addChild(container.createView(particleBitmap))
    container.apply {
        yellowStates.forEach { yellowState ->
            val sprite = initSprite(yellowState, particleTexture, Colors.YELLOW)
            spriteParticleStateMap[sprite.id] = yellowState
        }
        orangeStates.forEach { orangeState ->
            val sprite = initSprite(orangeState, particleTexture, Colors.ORANGE)
            spriteParticleStateMap[sprite.id] = orangeState
        }
        whiteStates.forEach { whiteState ->
            val sprite = initSprite(whiteState, particleTexture, Colors.WHITE)
            spriteParticleStateMap[sprite.id] = whiteState
        }
        redStates.forEach { redState ->
            val sprite = initSprite(redState, particleTexture, Colors.RED)
            spriteParticleStateMap[sprite.id] = redState
        }
        purpleStates.forEach { purpleState ->
            val sprite = initSprite(purpleState, particleTexture, Colors.MEDIUMPURPLE)
            spriteParticleStateMap[sprite.id] = purpleState
        }
    }

    addFixedUpdater(MainConstants.GUI_UPDATE_FREQ) {
        container.fastForEach { sprite ->
            with(spriteParticleStateMap[sprite.id]!!) {
                sprite.x = posX
                sprite.y = posY
                sprite.scaleX = scaleX
                sprite.scaleY = scaleY
                sprite.anchorX = anchorX
                sprite.anchorY = anchorY
            }
        }
    }
    coroutineScope { launch { stateCalculator.calculate(this, dispatcher) } }
}

private fun createParticleStates(): List<ParticleState> {
    val states = mutableListOf<ParticleState>()
    for (i in 1 .. MainConstants.PARTICLE_COUNT_PER_COLOR) {
        states.add(ParticleState(MainConstants.DEFAULT_PARTICLE_MASS, RandomUtil.randomXpos(), RandomUtil.randomYpos()))
    }

    return states
}

private fun Stage.drawBorder(thickness: Int) {
    solidRect(MainConstants.WIDTH - 2 * MainConstants.WALL_BUFFER, thickness)
        .xy(MainConstants.WALL_BUFFER, MainConstants.WALL_BUFFER)
    solidRect(MainConstants.WIDTH - 2 * MainConstants.WALL_BUFFER, thickness)
        .xy(MainConstants.WALL_BUFFER, MainConstants.HEIGHT - MainConstants.WALL_BUFFER - thickness)
    solidRect(thickness, MainConstants.HEIGHT - 2 * MainConstants.WALL_BUFFER)
        .xy(MainConstants.WALL_BUFFER, MainConstants.WALL_BUFFER)
    solidRect(thickness, MainConstants.HEIGHT - 2 * MainConstants.WALL_BUFFER)
        .xy(MainConstants.WIDTH - MainConstants.WALL_BUFFER - thickness, MainConstants.WALL_BUFFER)
}

@OptIn(KorgeExperimental::class)
private fun Stage.openPropertiesWindow(weights: ForceWeights, stateCalculator: StateCalculator) {
    uiWindow("Properties", 160.0, 215.0) {
        it.container.mobileBehaviour = true
        it.container.overflowRate = 0.0
        uiVerticalStack(160.0) {
            uiPropertyNumberRow("Gravity Well", *UIEditableNumberPropsList(stateCalculator::gravityWell,
                min = 10.0, max = 1000.0, clamped = true))
            uiButton(text = "Randomize Gravity") { onClick { weights.randomize() }}
            uiButton(text = "Gravity Nudge") { onClick { weights.randomOffset() }}
            uiButton(text = "Randomize Mass") { onClick { stateCalculator.randomizeMass() }}
            uiButton(text = "Reset Mass") { onClick { stateCalculator.setMass(MainConstants.DEFAULT_PARTICLE_MASS) }}
            uiButton(text = "Reset Position") { onClick { stateCalculator.resetPosition() }}
        }
    }.xy(0, 0)
}

private fun FSprites.initSprite(state: ParticleState, texture: BmpSlice, color: RGBA): FSprite {
    with(alloc()) {
        x = state.posX
        y = state.posY
        colorMul = color
        setTex(texture)
        scaleX = MainConstants.DEFAULT_PARTICLE_SCALE
        scaleY = MainConstants.DEFAULT_PARTICLE_SCALE
        anchorX = (MainConstants.BITMAP_SIZE * scaleX) / 2f
        anchorY = (MainConstants.BITMAP_SIZE * scaleY) / 2f

        return this
    }
}
