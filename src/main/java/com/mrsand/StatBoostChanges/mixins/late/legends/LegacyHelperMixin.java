package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;
import com.mrsand.StatBoostChanges.abilities.powers.BvVampirePowers;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Daywalker: when removeLegacyDaywalker is on, block learning; treat Vampirism
 * lords as owning Daywalker; force non-lords as not having it.
 * Intangibility: shown on tree, never purchasable; owned only by vampire lords.
 * Transvection: hover must not count as "using an ability" so other keys still fire.
 */
@Mixin(value = LegacyHelper.class, remap = false)
public abstract class LegacyHelperMixin {

	@Inject(method = "hasLearnedPower", at = @At("HEAD"), cancellable = true)
	private static void bvForcedOwnership(EntityPlayer player, LegacyPower power,
			CallbackInfoReturnable<Boolean> cir) {
		if (power == BvVampirePowers.intangibility) {
			cir.setReturnValue(Boolean.valueOf(DaywalkerControl.isVampireLord(player)));
			return;
		}
		if (!DaywalkerControl.isEnabled() || power != LegacyRegistry.daywalker) return;
		if (DaywalkerControl.isVampireLord(player)) {
			cir.setReturnValue(Boolean.TRUE);
			return;
		}
		cir.setReturnValue(Boolean.FALSE);
	}

	@Inject(method = "canBeLearned", at = @At("HEAD"), cancellable = true)
	private static void bvBlockForcedLearn(EntityPlayer player, LegacyPower power,
			CallbackInfoReturnable<Boolean> cir) {
		if (power == BvVampirePowers.intangibility) {
			cir.setReturnValue(Boolean.FALSE);
			return;
		}
		if (!DaywalkerControl.isEnabled() || power != LegacyRegistry.daywalker) return;
		cir.setReturnValue(Boolean.FALSE);
	}

	/** Treat TRANSVECTION as idle so hypnosis / bat attack / etc. can keyPress while hovering. */
	@Redirect(
			method = "isUsingAbility",
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
