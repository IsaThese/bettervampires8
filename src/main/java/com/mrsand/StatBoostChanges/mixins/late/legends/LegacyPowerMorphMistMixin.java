package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.powers.LegacyPowerMorphMist;
import com.tihyo.legends.morph.MorphHelper;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = LegacyPowerMorphMist.class, remap = false)
public abstract class LegacyPowerMorphMistMixin {

	@Shadow
	public abstract int getAssignedKey(EntityPlayer player);

	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
	public void execute(EntityPlayer player, CallbackInfo ci) {
		ci.cancel();
		if (!LegacyHelper.useAbility(player, getAssignedKey(player))) return;
		// Lords: mist allowed under roof by day even though getWeaknesses is daytime-true
		if (!VampireSunRules.canStartMist(player) || player.isInWater()) return;

		LegendsUtil.ifMorphedTurnHuman(player);
		SuspicionUtil.checkForSuspicionWitnesses(player, 500, 10.0D, "", true, false);
		// Indefinite mist (original applied mistForm(10))
		if (!LegendsUtil.isMist(player)) {
			LegendsUtil.activateMistFormIndefinite(player);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	public void increaseCooldownChange(EntityPlayer player, CallbackInfo ci) {
		ci.cancel();

		if (player.isInWater()) return;

		// Already mist: start 2s demist, or cancel demist to stay misted
		if (LegendsUtil.isMist(player)) {
			LegendsUtil.requestMistDemist(player);
			return;
		}

		int key = getAssignedKey(player);
		boolean offCooldown = LegacyHelper.getCooldown(player, key) == LegacyHelper.getCooldownMax(player, key);

		if (offCooldown && !MorphHelper.isMorphed((EntityLivingBase) player)) {
			if (player.getEntityData().getInteger("isInCombat") == 1) {
				if (WitcheryUtil.mistFormWhileInCombatPenalty(player, key)) {
					LegacyHelper.setDuration(player, key, 1);
				}
			} else {
				LegacyHelper.setCooldown(player, key, 0);
				LegacyHelper.setDuration(player, key, 1);
			}
		}
	}

}
