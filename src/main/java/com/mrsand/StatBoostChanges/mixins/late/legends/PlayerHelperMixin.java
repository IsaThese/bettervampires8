package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.LegendsAbilityBridge;
import com.tihyo.legends.management.LegendsGameRules;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = PlayerHelper.class, remap = false)
public abstract class PlayerHelperMixin {

	@Inject(method = "isInvincible", at = @At("HEAD"), cancellable = true)
	private static void isInvincibleChange(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		cir.cancel();
		cir.setReturnValue(
				LegendsAbilityBridge.isInvincible(player)
				|| LegendsAbilityBridge.isMist(player)
				|| LegendsAbilityBridge.getIntProp(player, LegendsProperties.POWER_WITHIN) > 0
				|| LegendsAbilityBridge.getIntProp(player, LegendsProperties.DEVONIAN_BEAST) > 0
				|| LegendsAbilityBridge.getIntProp(player, LegendsProperties.WEREWOLF_BITE) > 0
				|| LegendsAbilityBridge.getIntProp(player, LegendsProperties.BATON_BASH) > 0
				|| LegendsAbilityBridge.getIntProp(player, LegendsProperties.VADER_MAELSTROM) > 0
				|| LegendsAbilityBridge.getIntProp(player, LegendsProperties.CHOKE) > 0
				|| LegendsAbilityBridge.getIntProp(player, LegendsProperties.LIGHTNING_SHIELD) > 0
				|| player.worldObj.getGameRules().getGameRuleBooleanValue(LegendsGameRules.isInvincible));
	}
}
