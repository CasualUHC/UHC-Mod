package net.casual.championships.uhc.border

import net.casual.arcade.utils.TimeUtils.Minutes
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.world.level.Level

enum class UHCBorderStage(
    private val startSize: Double,
    private val endSize: Double,
    private val movingTime: MinecraftTimeDuration = MinecraftTimeDuration.ZERO,
    val pausedTime: MinecraftTimeDuration = MinecraftTimeDuration.ZERO
) {
    First(6128.0, 3064.0, 48.Minutes, 10.Minutes), // ~1.05BPS
    Second(First.endSize, 1532.0, 26.Minutes, 5.Minutes), // ~1BPS
    Third(Second.endSize, 510.0, 18.Minutes, 2.Minutes), // ~0.95BPS
    Fourth(Third.endSize, 102.0, 8.Minutes, 1.Minutes), // ~0.85BPS
    Fifth(Fourth.endSize, 20.0, 2.Minutes); // ~0.68BPS

    fun getRemainingMovingTime(size: Double, level: Level, multiplier: Double): MinecraftTimeDuration {
        val remainingSize = size - this.getEndSizeFor(level, multiplier)
        val totalSize = this.getStartSizeFor(level, multiplier) - this.getEndSizeFor(level, multiplier)
        return this.movingTime * (remainingSize / totalSize)
    }

    fun getRemainingMovingTimeAsPercent(size: Double, level: Level, multiplier: Double): Double {
        val remainingSize = size - this.getEndSizeFor(level, multiplier)
        val totalSize = this.getStartSizeFor(level, multiplier) - this.getEndSizeFor(level, multiplier)
        return (remainingSize / totalSize) * (this.movingTime / TOTAL_TIME)
    }

    fun getPausedTimeAsPercent(): Double {
        return this.pausedTime / TOTAL_TIME
    }

    fun getNextStage(): UHCBorderStage {
        return entries.getOrElse(this.ordinal + 1) { Fifth }
    }

    fun getStartSizeFor(level: Level, multiplier: Double): Double {
        return adjustSize(this.startSize, level) * multiplier
    }

    fun getEndSizeFor(level: Level, multiplier: Double): Double {
        return adjustSize(this.endSize, level) * multiplier
    }

    companion object {
        // 120 minutes
        val TOTAL_TIME = entries.fold(MinecraftTimeDuration.ZERO) { acc, stage -> acc + stage.movingTime + stage.pausedTime }

        fun adjustSize(size: Double, level: Level): Double {
            return size / level.dimensionType().coordinateScale
        }
    }
}