package net.casual.championships.common.mixin.event;

import net.casual.arcade.events.GlobalEventHandler;
import net.casual.championships.common.event.border.BorderPortalWithinBoundsEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PortalForcer.class)
public class PortalForcerMixin {
	@Shadow @Final private ServerLevel level;

	@Redirect(
		method = "createPortal",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/border/WorldBorder;isWithinBounds(Lnet/minecraft/core/BlockPos;)Z"
		)
	)
	private boolean isWithinWorldBorder(WorldBorder instance, BlockPos pos) {
		BorderPortalWithinBoundsEvent event = new BorderPortalWithinBoundsEvent(this.level.getWorldBorder(), this.level, pos);
		GlobalEventHandler.Server.broadcast(event);
		if (event.isCancelled()) {
			return event.result();
		}
		return instance.isWithinBounds(pos);
	}
}
