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
 * BetterVampires restored powers: shown on tree, never purchasable; owned only by
 * Vampirism vampire lords (Hypnosis Mastery, Demonic Form/Fury, True Form, Teleport,
 * Hellfire, Intangibility).
 * <p>
 * Form movement: {@code isUsingAbility} only treats TRANSVECTION and {@code Stat.dash}
 * as busy among movement effects. Demonic flight ({@code Stat.flight*}) and the speed
 * pulse ({@code Stat.speed}) are not read there. Ignore TRANSVECTION always (humanoid
 * keyed hover, demonic beast, true form) so hover does not block Bat Attack / bite /
 * toggle-off / other keys — same as {@code PlayerHelper.canPerformAbility}. Real casts
 * (VAMPIRE_BITE, HYPNOSIS, BAT_SWARMING, dash, etc.) still busy.
 */
@Mixin(value = LegacyHelper.class, remap = false)
public abstract class LegacyHelperMixin {

	@Inject(method = "hasLearnedPower", at = @At("HEAD"), cancellable = true)
	private static void bvForcedOwnership(EntityPlayer player, LegacyPower power,
			CallbackInfoReturnable<Boolean> cir) {
		if (BvVampirePowers.isLordOnlyPower(power)) {
			cir.setReturnValue(Boolean.valueOf(DaywalkerControl.isVampireLord(player)));
			return;
		}
		// Stock Vampire's Wrath: lords own it automatically (merged lord upgrade; not BV duplicate).
		if (power == LegacyRegistry.vampiresWrath && DaywalkerControl.isVampireLord(player)) {
			cir.setReturnValue(Boolean.TRUE);
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
		if (BvVampirePowers.isLordOnlyPower(power)) {
			cir.setReturnValue(Boolean.FALSE);
			return;
		}
		// Lords already own stock Wrath; hide purchase (parent bloodRaze would otherwise block).
		if (power == LegacyRegistry.vampiresWrath && DaywalkerControl.isVampireLord(player)) {
			cir.setReturnValue(Boolean.FALSE);
			return;
		}
		if (!DaywalkerControl.isEnabled() || power != LegacyRegistry.daywalker) return;
		cir.setReturnValue(Boolean.FALSE);
	}

	/**
	 * TRANSVECTION hover must not report as "using an ability" (any form).
	 * Flight and speed pulses are not checked by {@code isUsingAbility}.
	 */
	@Redirect(
			method = "isUsingAbility",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/properties/LegendsProperties;get(Lnet/minecraft/entity/Entity;)Ljava/lang/Object;"
			)
	)
	private static Object bvIgnoreFormMovementBusy(LegendsProperties props, Entity entity) {
		if (props == LegendsProperties.TRANSVECTION) {
			return Boolean.FALSE;
		}
		return props.get(entity);
	}
}
