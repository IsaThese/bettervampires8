package com.mrsand.StatBoostChanges.mixins.late.legends;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.common.ExtendedPlayer;
import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;
import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.LegendsAbilityBridge;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.PlayerDataUtil;
import com.mrsand.StatBoostChanges.util.VisualUtil;
import com.tihyo.horror.legacy.LegacyVampire;
import com.tihyo.legends.abilities.LegendsAbilities;
import com.tihyo.legends.abilities.LegendsImmunities;
import com.tihyo.legends.abilities.status.StatusAbility;
import com.tihyo.legends.effects.LegendsEffects;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.morph.MorphHelper;
import com.tihyo.legends.morph.MorphRegistry;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

@Mixin(value = LegacyVampire.class, remap = false)
public abstract class LegacyVampireMixin {

	@Shadow public abstract boolean getWeaknesses(EntityPlayer player);

	/**
	 * Legends 8 stubbed getWeaknesses to always false. With Daywalker removed,
	 * restore ability blocking: non-lords in sunlight; lords all daytime
	 * (mist/weather under-roof allowlist is handled outside getWeaknesses).
	 * Sun burn exemption for lords stays in {@link com.mrsand.StatBoostChanges.abilities.BvSunDamage}.
	 */
	@Inject(method = "getWeaknesses", at = @At("HEAD"), cancellable = true)
	private void bvSunWeakness(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		if (!DaywalkerControl.isEnabled()) return;
		cir.setReturnValue(Boolean.valueOf(VampireSunRules.generalPowersBlocked(player)));
	}

	/**
	 * L8 LegacyVampire.isPassive always true → always wall-climb.
	 * Drink+sneak toggle stores {@link PlayerKeys#WALL_CLIMB}; climb only while that flag is on.
	 */
	@Inject(method = "isPassive", at = @At("HEAD"), cancellable = true)
	private void bvWallClimbNotPassive(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(Boolean.FALSE);
	}

	@Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
	private void bvWallClimbFlag(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		if (player == null) {
			cir.setReturnValue(Boolean.FALSE);
			return;
		}
		VExtendedPlayer v = VExtendedPlayer.get(player);
		cir.setReturnValue(Boolean.valueOf(v != null && v.getBoolean(PlayerKeys.WALL_CLIMB)));
	}

	/**
	 * Legends 8 removed morphVampireBeast from the vampire whitelist.
	 * Restore it when Demonic Form is unlocked (same morph classic Dracula uses).
	 * Also whitelist Castlevania True Form when that power is unlocked.
	 */
	@Inject(method = "morphs", at = @At("RETURN"))
	private void allowVampireBeastMorph(EntityPlayer player, CallbackInfoReturnable<List<String>> cir) {
		List<String> morphs = cir.getReturnValue();
		if (morphs == null) return;
		if (VampireRestoredAbilities.hasDemonicForm(player)) {
			String beast = MorphRegistry.vampireBeast.getName();
			if (!morphs.contains(beast)) {
				morphs.add(beast);
			}
		}
		if (VampireRestoredAbilities.hasTrueForm(player)) {
			String trueForm = MorphRegistry.draculaTrueForm.getName();
			if (!morphs.contains(trueForm)) {
				morphs.add(trueForm);
			}
		}
	}

	@Inject(method = "stats", at = @At("HEAD"), cancellable = true)
	private void changeStats(EntityPlayer player, CallbackInfo cir) {
		cir.cancel();
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
		VampireRestoredAbilities.ensureAutoGranted(player);

		if (!LegendsAbilityBridge.hasBloodMeter(player)) {
			PlayerHelper.setBloodMeter(player, 0);
		}

		boolean weak = LegacyHelper.getLegacyClass(player) != null
				&& LegacyHelper.getLegacyClass(player).getWeaknesses(player);

		if (weak) {
			if (VampireSunRules.shouldForceClearForms(player)) {
				if (MorphHelper.isMorphed(player)) {
					LegendsUtil.ifMorphedTurnHuman(player);
				}
				VampireRestoredAbilities.clearDemonicFury(player);
				if (LegendsAbilityBridge.isBloodVision(player)) {
					PowersHandler.bloodVision(player, false);
				}
				// Fangs + vampire bite stay available in sun/day (original BV); do not clear.
				if (LegendsAbilityBridge.isBloodLust(player)) {
					LegendsAbilityBridge.clearBloodLust(player);
				}
			}
			if (VampireSunRules.shouldForceClearMist(player) && LegendsAbilityBridge.isMist(player)) {
				LegendsUtil.clearMistForm(player);
			}
			vPlayer.setBoolean(PlayerKeys.HYPNOTIZING, false);
			if (!vPlayer.getBoolean(PlayerKeys.IS_STARVING)) {
				VisualUtil.changeForm(player, false);
			}
		}

		boolean mistAllowed = LegendsAbilityBridge.isMist(player)
				&& !VampireSunRules.shouldForceClearMist(player);
		if (mistAllowed) {
			LegendsAbilities.flight2(player, 3);
		}

		if (!weak && MorphHelper.isVampireBeast(player) && !PlayerHelper.isClimbing(player)
				&& !PlayerHelper.isClimbingUpsidedown(player)) {
			LegendsAbilities.flightLegacy(player, 3);
			if (!(player.isPotionActive(Potion.moveSpeed) || player.isPotionActive(Potion.moveSlowdown))) {
				StatusAbility.speed.run(player, 8, 8);
			}
		}

		if (!MorphHelper.isMorphed(player)) {
			LegendsAbilities.stealth(player, true);
		}

		// Legends 8 removed LegacyHelper.getRank; gate on active vampire class instead.
		if (LegacyHelper.isActive(player)
				&& LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire
				&& !weak) {

				ItemStack itemHeld = player.getHeldItem();
				if (itemHeld == null || !PlayerDataUtil.isWeapon(itemHeld)) {
					applyBloodTierStrength(player, playerEx);
				}

				boolean wVampire = false;
				if (Loader.isModLoaded("witchery") && ExtendedPlayer.get(player).getVampireLevel() > 0) {
					wVampire = true;
				}

				if (!wVampire) {
					int bloodLustBoost = PlayerHelper.getBloodLustSpeedBoost(player);
					int speedAmp = player.isPotionActive(LegendsEffects.werewolfVenom.id)
							? ((MorphHelper.isVampireBeast(player) ? 15 : (5 + PlayerHelper.getBloodAmount(player) / 50 + bloodLustBoost)) / 2)
							: (MorphHelper.isVampireBeast(player) ? 15 : (5 + PlayerHelper.getBloodAmount(player) / 50 + bloodLustBoost));
					StatusAbility.speed.run(player, speedAmp, -1 + bloodLustBoost);
				}

				if (!MorphHelper.isWolf(player)) {
					int acro = player.isPotionActive(LegendsEffects.werewolfVenom.id)
							? ((MorphHelper.isVampireBeast(player) ? 6 : 4) / 2)
							: (MorphHelper.isVampireBeast(player) ? 6 : 4);
					StatusAbility.acrobatics.run(player, acro);
				}

				LegendsAbilities.mentalDefense(player, 3);
				LegendsAbilities.regeneration(player, 0);
				if (!playerEx.isVampire()) {
					applyStrength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id)
							? ((MorphHelper.isVampireBeast(player) ? 20 : (MorphHelper.isWolf(player)
									? ((14 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (13 + PlayerHelper.getBloodAmount(player) / 20))) / 2)
							: (MorphHelper.isVampireBeast(player) ? 20 : (MorphHelper.isWolf(player)
									? ((14 + PlayerHelper.getBloodAmount(player) / 20) / 2)
									: (14 + PlayerHelper.getBloodAmount(player) / 20))));
					PowersHandler.nightVision(player, 220, 2);
				}
			LegendsAbilities.terror(player, 14);
			LegendsAbilities.composure(player, true);
			LegendsImmunities.poison(player);
			LegendsImmunities.wither(player);
			LegendsImmunities.fallDamage(player);

			if (MorphHelper.isWolf(player)) {
				LegendsAbilities.stepAssist(player, 1);
				StatusAbility.speed.run(player, 17, 17);
				StatusAbility.acrobatics.run(player, 5);
			}
		}
	}

	private static void applyBloodTierStrength(EntityPlayer player, ExtendedPlayer playerEx) {
		int bp = playerEx.getBloodPower();
		boolean venom = player.isPotionActive(LegendsEffects.werewolfVenom.id);
		if (bp < 600 && bp >= 1) {
			applyStrength(player, venom
					? ((MorphHelper.isVampireBeast(player) ? 8 : (MorphHelper.isWolf(player)
							? ((5 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (5 + PlayerHelper.getBloodAmount(player) / 20))) / 2)
					: (MorphHelper.isVampireBeast(player) ? 8 : (MorphHelper.isWolf(player)
							? ((5 + PlayerHelper.getBloodAmount(player) / 20) / 2)
							: (5 + PlayerHelper.getBloodAmount(player) / 20))));
		} else if (bp < 900 && bp >= 600) {
			applyStrength(player, venom
					? ((MorphHelper.isVampireBeast(player) ? 9 : (MorphHelper.isWolf(player)
							? ((6 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (6 + PlayerHelper.getBloodAmount(player) / 20))) / 2)
					: (MorphHelper.isVampireBeast(player) ? 9 : (MorphHelper.isWolf(player)
							? ((6 + PlayerHelper.getBloodAmount(player) / 20) / 2)
							: (6 + PlayerHelper.getBloodAmount(player) / 20))));
		} else if (bp < 1200 && bp >= 900) {
			applyStrength(player, venom
					? ((MorphHelper.isVampireBeast(player) ? 11 : (MorphHelper.isWolf(player)
							? ((8 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (8 + PlayerHelper.getBloodAmount(player) / 20))) / 2)
					: (MorphHelper.isVampireBeast(player) ? 11 : (MorphHelper.isWolf(player)
							? ((8 + PlayerHelper.getBloodAmount(player) / 20) / 2)
							: (8 + PlayerHelper.getBloodAmount(player) / 20))));
		} else if (bp < 1500 && bp >= 1200) {
			applyStrength(player, venom
					? ((MorphHelper.isVampireBeast(player) ? 13 : (MorphHelper.isWolf(player)
							? ((9 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (10 + PlayerHelper.getBloodAmount(player) / 20))) / 2)
					: (MorphHelper.isVampireBeast(player) ? 13 : (MorphHelper.isWolf(player)
							? ((9 + PlayerHelper.getBloodAmount(player) / 20) / 2)
							: (9 + PlayerHelper.getBloodAmount(player) / 20))));
		} else if (bp < 1800 && bp >= 1500) {
			applyStrength(player, venom
					? ((MorphHelper.isVampireBeast(player) ? 15 : (MorphHelper.isWolf(player)
							? ((11 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (11 + PlayerHelper.getBloodAmount(player) / 20))) / 2)
					: (MorphHelper.isVampireBeast(player) ? 15 : (MorphHelper.isWolf(player)
							? ((11 + PlayerHelper.getBloodAmount(player) / 20) / 2)
							: (11 + PlayerHelper.getBloodAmount(player) / 20))));
		} else if (bp < 2400 && bp >= 1800) {
			applyStrength(player, venom
					? ((MorphHelper.isVampireBeast(player) ? 17 : (MorphHelper.isWolf(player)
							? ((12 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (13 + PlayerHelper.getBloodAmount(player) / 20))) / 2)
					: (MorphHelper.isVampireBeast(player) ? 17 : (MorphHelper.isWolf(player)
							? ((12 + PlayerHelper.getBloodAmount(player) / 20) / 2)
							: (12 + PlayerHelper.getBloodAmount(player) / 20))));
		} else if (bp < 2700 && bp >= 2400) {
			applyStrength(player, venom
					? ((MorphHelper.isVampireBeast(player) ? 19 : (MorphHelper.isWolf(player)
							? ((13 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (13 + PlayerHelper.getBloodAmount(player) / 20))) / 2)
					: (MorphHelper.isVampireBeast(player) ? 19 : (MorphHelper.isWolf(player)
							? ((13 + PlayerHelper.getBloodAmount(player) / 20) / 2)
							: (13 + PlayerHelper.getBloodAmount(player) / 20))));
		} else if (bp < 3300 && bp >= 2700) {
			applyStrength(player, venom
					? ((MorphHelper.isVampireBeast(player) ? 20 : (MorphHelper.isWolf(player)
							? ((14 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (13 + PlayerHelper.getBloodAmount(player) / 20))) / 2)
					: (MorphHelper.isVampireBeast(player) ? 20 : (MorphHelper.isWolf(player)
							? ((14 + PlayerHelper.getBloodAmount(player) / 20) / 2)
							: (14 + PlayerHelper.getBloodAmount(player) / 20))));
		}
	}

	private static void applyStrength(EntityPlayer player, int amp) {
		StatusAbility.strength.run(player, amp);
	}
}
