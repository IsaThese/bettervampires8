package com.mrsand.StatBoostChanges.abilities;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;

/**
 * BetterVampires-accurate sun fire gates (matches BetterMinecraftVampires).
 */
public final class BvSunDamage {

	private BvSunDamage() {}

	public static boolean isEnabled() {
		return StatBoostConfig.bvAccurateSunDamage;
	}

	/**
	 * Skip Legends/Horror sun fire: Vampirism lord, lord+full clothes, or
	 * Witchery vampire level &gt;= 5 (debuff-only at those levels).
	 */
	public static boolean shouldSkipSunFire(EntityPlayer player) {
		if (player == null) return true;
		if (DaywalkerControl.isVampireLord(player)) return true;
		if (!Loader.isModLoaded("witchery")) return false;
		if (WitcheryUtil.hasFullSunImmunity(player)) return true;
		return WitcheryUtil.isVampire(player) && WitcheryUtil.getVampireLevel(player) >= 5;
	}

	/** Config on and player should burn in sun (helmet must not block). */
	public static boolean shouldBurnInSun(EntityPlayer player) {
		return isEnabled() && !shouldSkipSunFire(player);
	}
}
