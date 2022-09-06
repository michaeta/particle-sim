

data class ForceWeights(
    var yellowWhite: Float = RandomUtil.randomWeight(),
    var yellowOrange: Float = RandomUtil.randomWeight(),
    var yellowYellow: Float = RandomUtil.randomWeight(),
    var yellowRed: Float = RandomUtil.randomWeight(),
    var yellowPurple: Float = RandomUtil.randomWeight(),
    var orangeWhite: Float = RandomUtil.randomWeight(),
    var orangeOrange: Float = RandomUtil.randomWeight(),
    var orangeYellow: Float = RandomUtil.randomWeight(),
    var orangeRed: Float = RandomUtil.randomWeight(),
    var orangePurple: Float = RandomUtil.randomWeight(),
    var whiteWhite: Float = RandomUtil.randomWeight(),
    var whiteOrange: Float = RandomUtil.randomWeight(),
    var whiteYellow: Float = RandomUtil.randomWeight(),
    var whiteRed: Float = RandomUtil.randomWeight(),
    var whitePurple: Float = RandomUtil.randomWeight(),
    var redWhite: Float = RandomUtil.randomWeight(),
    var redOrange: Float = RandomUtil.randomWeight(),
    var redYellow: Float = RandomUtil.randomWeight(),
    var redRed: Float = RandomUtil.randomWeight(),
    var redPurple: Float = RandomUtil.randomWeight(),
    var purpleWhite: Float = RandomUtil.randomWeight(),
    var purpleOrange: Float = RandomUtil.randomWeight(),
    var purpleYellow: Float = RandomUtil.randomWeight(),
    var purpleRed: Float = RandomUtil.randomWeight(),
    var purplePurple: Float = RandomUtil.randomWeight()
)

fun ForceWeights.randomize() {
    yellowWhite = RandomUtil.randomWeight()
    yellowOrange = RandomUtil.randomWeight()
    yellowYellow = RandomUtil.randomWeight()
    yellowRed = RandomUtil.randomWeight()
    yellowPurple = RandomUtil.randomWeight()
    orangeWhite = RandomUtil.randomWeight()
    orangeOrange = RandomUtil.randomWeight()
    orangeYellow = RandomUtil.randomWeight()
    orangeRed = RandomUtil.randomWeight()
    orangePurple = RandomUtil.randomWeight()
    whiteWhite = RandomUtil.randomWeight()
    whiteOrange = RandomUtil.randomWeight()
    whiteYellow = RandomUtil.randomWeight()
    whiteRed = RandomUtil.randomWeight()
    whitePurple = RandomUtil.randomWeight()
    redWhite = RandomUtil.randomWeight()
    redOrange = RandomUtil.randomWeight()
    redYellow = RandomUtil.randomWeight()
    redRed = RandomUtil.randomWeight()
    redPurple = RandomUtil.randomWeight()
    purpleWhite = RandomUtil.randomWeight()
    purpleOrange = RandomUtil.randomWeight()
    purpleYellow = RandomUtil.randomWeight()
    purpleRed = RandomUtil.randomWeight()
    purplePurple = RandomUtil.randomWeight()
}

fun ForceWeights.randomOffset() {
    yellowWhite += RandomUtil.smallRandomWeight()
    yellowOrange += RandomUtil.smallRandomWeight()
    yellowYellow += RandomUtil.smallRandomWeight()
    yellowRed += RandomUtil.smallRandomWeight()
    yellowPurple += RandomUtil.smallRandomWeight()
    orangeWhite += RandomUtil.smallRandomWeight()
    orangeOrange += RandomUtil.smallRandomWeight()
    orangeYellow += RandomUtil.smallRandomWeight()
    orangeRed += RandomUtil.smallRandomWeight()
    orangePurple += RandomUtil.smallRandomWeight()
    whiteWhite += RandomUtil.smallRandomWeight()
    whiteOrange += RandomUtil.smallRandomWeight()
    whiteYellow += RandomUtil.smallRandomWeight()
    whiteRed += RandomUtil.smallRandomWeight()
    whitePurple += RandomUtil.smallRandomWeight()
    redWhite += RandomUtil.smallRandomWeight()
    redOrange += RandomUtil.smallRandomWeight()
    redYellow += RandomUtil.smallRandomWeight()
    redRed += RandomUtil.smallRandomWeight()
    redPurple += RandomUtil.smallRandomWeight()
    purpleWhite += RandomUtil.smallRandomWeight()
    purpleOrange += RandomUtil.smallRandomWeight()
    purpleYellow += RandomUtil.smallRandomWeight()
    purpleRed += RandomUtil.smallRandomWeight()
    purplePurple += RandomUtil.smallRandomWeight()
}

