import com.soywiz.klock.*
import kotlin.random.*

class RandomUtil {
    companion object {
        private val NUMBER_GENERATOR = Random(DateTime.nowUnixLong())

        fun randomWeight(): Float = (NUMBER_GENERATOR.nextFloat() - 0.5f) / 0.75f

        fun smallRandomWeight(): Float = randomWeight() / 3.0f

        fun randomMass(): Float = 3f * (randomWeight() + 1f)

        fun randomXpos(): Float = NUMBER_GENERATOR.nextInt((MainConstants.WIDTH / 1.5).toInt()).toFloat() + MainConstants.WIDTH / 6

        fun randomYpos(): Float = NUMBER_GENERATOR.nextInt((MainConstants.HEIGHT / 1.5).toInt()).toFloat() + MainConstants.HEIGHT / 6
    }
}
