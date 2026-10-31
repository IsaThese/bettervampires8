package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrsand.StatBoostChanges.util.VampirismUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.handlers.LegendsCommonEventHandler;
import com.tihyo.legends.handlers.PowersHandler;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Legends 8 removed AbilityHelper-based hooks used by the old mixin.
 * Mist phasing / bite / hypnosis mastery now live in BetterVampires handlers.
 * Keep only sunlight redirects here.
 */
@Mixin(value = LegendsCommonEventHandler.class, remap = false)
public abstract class LegendsCommonEventHandlerMixin {

	@Redirect(method = "onPlayerUpdate", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/player/EntityPlayer;func_70015_d(I)V"))
	private void noFireVampires(EntityPlayer player, int ticks) {
		if (Loader.isModLoaded("witchery")) {
			if (WitcheryUtil.hasFullSunImmunity(player) || WitcheryUtil.getVampireLevel(player) >= 5) {
				return;
			}
		} else {
			player.setFire(1);
		}
	}

	@Redirect(method = "onPlayerUpdate", at = @At(value = "INVOKE",
			target = "Lcom/tihyo/legends/handlers/PowersHandler;sunlight(Lnet/minecraft/entity/player/EntityPlayer;I)V"))
	private void noSunlightInOtherDimensions(EntityPlayer player, int ticks) {
		if (WitcheryUtil.hasFullSunImmunity(player)) {
			return;
		}
		if (player.worldObj.provider.dimensionId == 0) {
			if (Loader.isModLoaded("vampirism")) {
				if (!VampirismUtil.isInVampireBiome(player) && player.worldObj.isDaytime()) {
					PowersHandler.sunlight(player, ticks);
				}
			} else {
				PowersHandler.sunlight(player, ticks);
			}
		}
	}
}
