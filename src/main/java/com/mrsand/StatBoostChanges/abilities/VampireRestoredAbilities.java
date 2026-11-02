package com.mrsand.StatBoostChanges.abilities;

import java.util.List;

import com.mrsand.StatBoostChanges.abilities.powers.BvVampirePowers;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.LegendsAbilityBridge;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.WebPassDebug;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.management.properties.PropertyManager;

import net.minecraft.entity.player.EntityPlayer;

/**
 * BetterVampires restorations for powers removed in Legends 8.
 * All BV powers are Vampirism vampire-lord-only (shown on tree, not purchasable).
 */
public final class VampireRestoredAbilities {

	public static final int DEFAULT_HYPNOSIS_STRENGTH = 100;
	public static final int DEFAULT_FURY_SECONDS = 15;

	private VampireRestoredAbilities() {}

	public static boolean isVampire(EntityPlayer player) {
		return player != null && LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire;
	}

	/**
	 * Lord baseline + strip BV powers from the learned list (ownership is forced via
	 * {@code LegacyHelperMixin} for lords; non-lords must not keep stale entries).
	 */
	public static void ensureAutoGranted(EntityPlayer player) {
		if (!isVampire(player) || player.worldObj.isRemote) return;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null) return;

		boolean changed = false;
		boolean lord = DaywalkerControl.isVampireLord(player);
		if (lord) {
			if (!v.getBoolean(PlayerKeys.UNLOCKED_RESTORED_POWERS)) {
				v.setBoolean(PlayerKeys.UNLOCKED_RESTORED_POWERS, true);
				changed = true;
			}
			if (v.getInteger(PlayerKeys.HYPNOSIS_STRENGTH) <= 0) {
				v.setInteger(PlayerKeys.HYPNOSIS_STRENGTH, DEFAULT_HYPNOSIS_STRENGTH);
				changed = true;
			}
		}

		// Do NOT persist BV powers in the learned list — lord ownership is virtual.
		// Strip stale auto-grants from earlier builds so non-lords lose them.
		List<LegacyPower> learned = LegacyHelper.getLearnedPowers(player);
		boolean powersChanged = false;
		for (LegacyPower power : BvVampirePowers.all()) {
			if (learned.remove(power)) {
				powersChanged = true;
				changed = true;
			}
		}
		if (DaywalkerControl.isEnabled() && LegacyRegistry.daywalker != null
				&& learned.contains(LegacyRegistry.daywalker)) {
			learned.remove(LegacyRegistry.daywalker);
			powersChanged = true;
			changed = true;
		}
		if (powersChanged) {
			PropertyManager.setUnlockedPowers(player, learned);
		}

		if (changed) {
			v.syncClient();
		}
	}

	public static boolean hasRestoredPowers(EntityPlayer player) {
		return isVampire(player) && DaywalkerControl.isVampireLord(player);
	}

	public static boolean hasDemonicForm(EntityPlayer player) {
		return DaywalkerControl.isVampireLord(player);
	}

	public static boolean hasHypnosisMastery(EntityPlayer player) {
		return DaywalkerControl.isVampireLord(player);
	}

	/** Cobweb pass-through — Vampirism vampire lords only (shown in Legends, not learnable). */
	public static boolean hasIntangibility(EntityPlayer player) {
		return DaywalkerControl.isVampireLord(player);
	}

	public static boolean hasDemonicFury(EntityPlayer player) {
		return DaywalkerControl.isVampireLord(player);
	}

	public static boolean hasTrueForm(EntityPlayer player) {
		return DaywalkerControl.isVampireLord(player);
	}

	public static boolean hasDraculaTeleport(EntityPlayer player) {
		return DaywalkerControl.isVampireLord(player);
	}

	public static boolean hasHellfire(EntityPlayer player) {
		return DaywalkerControl.isVampireLord(player);
	}

	public static int getHypnosisStrength(EntityPlayer player) {
		VExtendedPlayer v = VExtendedPlayer.get(player);
		return v == null ? 0 : v.getInteger(PlayerKeys.HYPNOSIS_STRENGTH);
	}

	public static void setHypnosisStrength(EntityPlayer player, int amount) {
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null || player.worldObj.isRemote) return;
		int next = LegendsHelper.limit(amount, 0, DEFAULT_HYPNOSIS_STRENGTH);
		if (v.getInteger(PlayerKeys.HYPNOSIS_STRENGTH) == next) return;
		v.setInteger(PlayerKeys.HYPNOSIS_STRENGTH, next);
		v.syncClient();
	}

	public static void addHypnosisStrength(EntityPlayer player, int delta) {
		setHypnosisStrength(player, getHypnosisStrength(player) + delta);
	}

	/** True while Hypnosis Mastery hold duration is active. */
	public static boolean isHypnosisMasteryChanneling(EntityPlayer player) {
		if (player == null || !hasHypnosisMastery(player)) return false;
		int key = BvVampirePowers.hypnosisMastery.getAssignedKey(player);
		return key > 0 && LegacyHelper.getDuration(player, key) > 0;
	}

	public static boolean isDemonicFuryActive(EntityPlayer player) {
		VExtendedPlayer v = VExtendedPlayer.get(player);
		return v != null && v.getInteger(PlayerKeys.DEMONIC_FURY_TICKS) > 0;
	}

	public static void activateDemonicFury(EntityPlayer player, int seconds) {
		if (player == null || player.worldObj.isRemote || !hasDemonicFury(player)) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null) return;
		int ticks = Math.max(1, seconds) * 20;
		v.setInteger(PlayerKeys.DEMONIC_FURY_TICKS, Math.max(v.getInteger(PlayerKeys.DEMONIC_FURY_TICKS), ticks));
		PowersHandler.fury(player, seconds);
		v.syncClient();
	}

	/** End fury immediately (sun/daytime strip). */
	public static void clearDemonicFury(EntityPlayer player) {
		if (player == null || player.worldObj.isRemote) return;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v != null && v.getInteger(PlayerKeys.DEMONIC_FURY_TICKS) > 0) {
			v.setInteger(PlayerKeys.DEMONIC_FURY_TICKS, 0);
			v.syncClient();
		}
		if (LegendsAbilityBridge.isFury(player)) {
			com.tihyo.legends.management.properties.LegendsProperties.FURY.set(player, Integer.valueOf(0));
		}
	}

	public static void tick(EntityPlayer player) {
		if (player == null || player.worldObj.isRemote) return;
		ensureAutoGranted(player);
		if (!hasRestoredPowers(player)) return;

		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null) return;

		int fury = v.getInteger(PlayerKeys.DEMONIC_FURY_TICKS);
		if (fury > 0) {
			v.setInteger(PlayerKeys.DEMONIC_FURY_TICKS, fury - 1);
			if (fury - 1 == 0) {
				v.syncClient();
			}
		}

		tickHypnosisStrengthRegen(player);
		HypnosisMasteryLogic.tickSessionExpiry(player);
	}

	/** Legends 5.4: +1 hypnotic strength every 5 ticks while not controlling. */
	private static void tickHypnosisStrengthRegen(EntityPlayer player) {
		if (!hasHypnosisMastery(player)) return;
		if (isHypnosisMasteryChanneling(player)) return;
		if (getHypnosisStrength(player) >= DEFAULT_HYPNOSIS_STRENGTH) return;
		if (LegendsHelper.getTicks(player) % 5 != 0) return;
		addHypnosisStrength(player, 1);
	}

	/**
	 * Passive intangibility gate (cobwebs only). Works anytime for lords — not sun/day gated.
	 * Cobwebs use {@code Entity.isInWeb} slowdown, not solid collision.
	 */
	public static boolean shouldPassThroughWebs(EntityPlayer player) {
		return player != null && hasIntangibility(player);
	}

	/**
	 * Client+server fallback: clear {@code isInWeb} via reflection before/around movement.
	 * Mixins are preferred; this covers cases where early injects miss due to refmap gaps.
	 */
	public static void tickWebPassThrough(EntityPlayer player) {
		if (player == null) return;
		WebPassDebug.sample(player, "tick");
		if (!shouldPassThroughWebs(player)) return;
		WebPassDebug.clearIsInWeb(player);
	}
}
