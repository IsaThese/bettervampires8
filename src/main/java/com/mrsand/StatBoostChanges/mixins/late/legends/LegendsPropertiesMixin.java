package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * PlayerDataPacket applies props via {@code setWithoutNotify}, which skips
 * {@code PlayerHelper.setInCoffin} → BV leave. Run full client leave when
 * {@code IS_IN_COFFIN} becomes false on that path.
 */
@Mixin(value = LegendsProperties.class, remap = false)
public abstract class LegendsPropertiesMixin {

	@Inject(method = "setWithoutNotify", at = @At("RETURN"))
	private void bvCoffinFlagPacketLeave(Entity entity, Object value, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this != LegendsProperties.IS_IN_COFFIN) return;
		if (!(entity instanceof EntityPlayer)) return;
		if (!(value instanceof Boolean) || ((Boolean) value).booleanValue()) return;
		EntityPlayer player = (EntityPlayer) entity;
		if (player.worldObj == null || !player.worldObj.isRemote) return;
		if (CoffinSleepUtil.isExitAllowed()) return;
		CoffinSleepUtil.onNestFlagCleared(player);
	}
}
