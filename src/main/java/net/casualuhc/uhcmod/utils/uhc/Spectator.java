package net.casualuhc.uhcmod.utils.uhc;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

// Blame Kerb
@Deprecated
public class Spectator {
    private static ServerPlayerEntity cameraman;

    public static void spectate(ServerPlayerEntity from, ServerPlayerEntity player) {
        if (!from.isSpectator()) {
            return;
        }
        from.teleport(
            player.getWorld(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYaw(),
            player.getPitch()
        );
    }

    public static void spectate(ServerPlayerEntity from) throws CommandSyntaxException {
        if (cameraman == null) {
            throw new SimpleCommandExceptionType(Text.literal("Cameraman has not been set!")).create();
        }
        spectate(from, cameraman);
    }

    public static void spectate(ServerPlayerEntity from, Team team) throws CommandSyntaxException {
        ServerPlayerEntity player = getAnyAlive(from.server, team);
        if (player == null) {
            throw new SimpleCommandExceptionType(Text.literal("There are no players to spectate!")).create();
        }
        spectate(from, player);
    }

    public static ServerPlayerEntity getAnyAlive(MinecraftServer server, Team team){
        return server.getPlayerManager()
            .getPlayerList()
            .stream()
            .filter(player -> player.isTeamPlayer(team))
            .filter(player -> !player.isSpectator())
            .findAny()
            .orElse(cameraman);
    }

    public static void setCameraman(ServerPlayerEntity player) {
        cameraman = player;
    }

    public static ServerPlayerEntity getCameraman(){
        return cameraman;
    }
}