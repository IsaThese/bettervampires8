package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;
import com.mrsand.StatBoostChanges.util.LegendsAbilityBridge;
import com.mrsand.StatBoostChanges.util.VampireBloodChannels;
import com.tihyo.legends.management.LegendsGameRules;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = PlayerHelper.class, remap = false)
public abstract class PlayerHelperMixin {

	@Inject(method = "setInCoffin", at = @At("RETURN"))
	private static void bvAfterSetInCoffin(EntityPlayer player, boolean inCoffin, float yaw, CallbackInfo ci) {
		if (player == null || player.worldObj == null) return;
		if (inCoffin) {
			CoffinSleepUtil.afterSetInCoffinTrue(player);
		} else if (!CoffinSleepUtil.isExitAllowed()) {
			// Legends cleared the flag without BV exitCoffin — full leave (size/eye/rot/teleport).
			CoffinSleepUtil.onNestFlagCleared(player);
		}
	}

	/**
	 * Coffin nest must not trigger Legends {@code cantMove} (head lock + slowness).
	 * Keep real lying and bath.
	 */
	@Redirect(
			method = "cantMove",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/PlayerHelper;isLying(Lnet/minecraft/entity/player/EntityPlayer;)Z"
			)
	)
	private static boolean bvCantMoveIsLyingIgnoreCoffin(EntityPlayer player) {
		return CoffinSleepUtil.isLyingIgnoringCoffin(player);
	}

	/**
	 * Stock Vampiric Drain grants +20 on the same look-at hit as damage.
	 * Retune that amount to slightly under Vampiric Bite while drain is active.
	 */
	@ModifyVariable(method = "increaseBloodMeter", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static int bvRetuneDrainBloodGrant(int amount, EntityPlayer player) {
		return VampireBloodChannels.retuneDrainBloodAmount(player, amount);
	}

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

	/** Allow other ability keys while Transvection hover is active. */
	@Redirect(
			method = "canPerformAbility",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/properties/LegendsProperties;get(Lnet/minecraft/entity/Entity;)Ljava/lang/Object;"
			)
	)
	private static Object bvIgnoreTransvectionBusy(LegendsProperties props, Entity entity) {
		if (props == LegendsProperties.TRANSVECTION) {
			return Boolean.FALSE;
		}
		return props.get(entity);
	}
}
