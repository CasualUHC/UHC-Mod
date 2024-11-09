package net.casual.championships.common.task

import com.google.gson.JsonObject
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.task.MinigameTaskCreationContext
import net.casual.arcade.minigame.task.MinigameTaskFactory
import net.casual.arcade.minigame.task.impl.BossbarTask
import net.casual.arcade.scheduler.task.SavableTask
import net.casual.arcade.scheduler.task.Task
import net.casual.arcade.scheduler.task.serialization.TaskSerializationContext
import net.casual.championships.common.CommonMod
import net.casual.championships.common.ui.bossbar.GraceBossbar

class GracePeriodBossbarTask(
    minigame: Minigame
): BossbarTask<GraceBossbar>(minigame, GraceBossbar()), SavableTask {
    override val id = Companion.id

    override fun serialize(context: TaskSerializationContext): JsonObject {
        return this.bar.writeData(context)
    }

    companion object: MinigameTaskFactory<Minigame> {
        override val id = CommonMod.id("grace_period_boss_bar_task")

        override fun create(context: MinigameTaskCreationContext<Minigame>): Task {
            return GracePeriodBossbarTask(context.minigame).readData(context)
        }
    }
}