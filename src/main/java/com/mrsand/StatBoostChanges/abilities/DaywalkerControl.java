package com.mrsand.StatBoostChanges.abilities;

import java.util.List;

import com.mrsand.StatBoostChanges.ModCompat;
import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.util.VampirismUtil;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.legacy.LegendsLegacy;
import com.tihyo.legends.management.properties.PropertyManager;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Removes Legends Daywalker when {@link StatBoostConfig#removeLegacyDaywalker} is true,
 * and treats Vampirism lords as Daywalker-safe for <em>sun burn</em> checks only.
 * Daytime ability limits for lords are in {@link VampireSunRules}.
 */
public final class DaywalkerControl {

	private DaywalkerControl() {}

	public static boolean isEnabled() {
		return StatBoostConfig.removeLegacyDaywalker;
	}

	public static boolean isVampireLord(EntityPlayer player) {
		return ModCompat.VAMPIRISM && VampirismUtil.isVampireLord(player);
	}

	/** Drop Daywalker from the vampire skill tree after Legends powers register. */
	public static void stripFromVampireTree() {
		if (!isEnabled() || !ModCompat.LEGENDS) return;
		LegacyPower daywalker = LegacyRegistry.daywalker;
		if (daywalker == null || LegacyRegistry.vampire == null) return;
		List<LegendsLegacy> legacies = daywalker.getLegacies();
		if (legacies == null) return;
		LegacyHelper.removePowerFromLegacy(daywalker, legacies, LegacyRegistry.vampire);
	}

	/** Remove Daywalker from a vampire's unlocked powers list if present. */
	public static boolean stripFromPlayer(EntityPlayer player) {
		if (!isEnabled() || player == null || player.worldObj.isRemote) return false;
		LegacyPower daywalker = LegacyRegistry.daywalker;
		if (daywalker == null) return false;
		List<LegacyPower> learned = LegacyHelper.getLearnedPowers(player);
		if (learned == null || !learned.contains(daywalker)) return false;
		learned.remove(daywalker);
		PropertyManager.setUnlockedPowers(player, learned);
		return true;
	}
}
