package net.casual.championships.common.mixin.event;

import com.llamalad7.mixinextras.sugar.Local;
import net.casual.arcade.events.GlobalEventHandler;
import net.casual.championships.common.event.border.BorderEntityPortalEntryPointEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
	@Redirect(
		method = "getPortalDestination",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/border/WorldBorder;clampToBounds(DDD)Lnet/minecraft/core/BlockPos;"
		)
	)
	private BlockPos clampWithinDistanceOfWorldBorder(
		WorldBorder border,
		double x,
		double y,
		double z,
		@Local(ordinal = 1) ServerLevel level,
		@Local(argsOnly = true) Entity entity
	) {
		BorderEntityPortalEntryPointEvent event = new BorderEntityPortalEntryPointEvent(border, level, entity, new Vec3(x, y, z));
		GlobalEventHandler.Server.broadcast(event);
		if (event.isCancelled()) {
			return event.result();
		}
		return border.clampToBounds(x, y, z);
	}
}
