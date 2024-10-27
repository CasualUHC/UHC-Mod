package net.casual.championships.common.entities

import eu.pb4.polymer.core.api.entity.PolymerEntity
import net.casual.championships.common.util.CommonEntities
import net.casual.championships.common.util.CommonItems
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import xyz.nucleoid.packettweaker.PacketContext

class MysteriousPearl: ThrowableItemProjectile, PolymerEntity {
    constructor(type: EntityType<out MysteriousPearl>, level: Level): super(type, level)

    constructor(level: Level, shooter: LivingEntity):
        super(CommonEntities.MYSTERIOUS_PEARL, shooter, level, ItemStack(CommonItems.MYSTERIOUS_PEARL))

    override fun getPolymerEntityType(context: PacketContext): EntityType<*> {
        return EntityType.SNOWBALL
    }

    override fun getDefaultItem(): Item {
        return Items.AIR
    }

    override fun onHit(result: HitResult) {
        super.onHit(result)

        this.level().explode(this, this.x, this.y, this.z, 3F, Level.ExplosionInteraction.NONE)
    }
}