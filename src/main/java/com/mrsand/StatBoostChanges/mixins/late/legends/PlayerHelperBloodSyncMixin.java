package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.BloodMeterSync;
import com.tihyo.legends.management.PlayerHelper;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Loaded only when Witchery + Legends are present. Mirrors Legends blood onto
 * Witchery blood power.
 */
@Mixin(value = PlayerHelper.class, remap = false)
public abstract class PlayerHelperBloodSyncMixin {

	@Inject(method = "setBloodMeter", at = @At("RETURN"))
	private static void betterVampires$syncWitcheryBlood(EntityPlayer player, int amount, CallbackInfo ci) {
		BloodMeterSync.syncWitcheryFromLegends(player, amount);
	}
}
