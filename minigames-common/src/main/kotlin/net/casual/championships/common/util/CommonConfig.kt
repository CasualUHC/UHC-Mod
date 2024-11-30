package net.casual.championships.common.util

import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path

object CommonConfig {
    private val root = FabricLoader.getInstance().configDir.resolve("CasualChampionships")

    fun resolve(next: String): Path {
        return this.root.resolve(next)
    }
}