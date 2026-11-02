package com.mrsand.StatBoostChanges.util;

import com.emoniph.witchery.common.ExtendedPlayer;
import com.tihyo.legends.management.PlayerHelper;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/**
 * Keeps Witchery {@code bloodPower} and Legends {@code BLOOD_AMOUNT} (0–100)
 * aligned as percentages. Re-entrancy guarded so one write does not recurse.
 */
public final class BloodMeterSync {

	private static final ThreadLocal<Boolean> SYNCING = new ThreadLocal<Boolean>();

	private BloodMeterSync() {}

	private static boolean begin() {
		if (Boolean.TRUE.equals(SYNCING.get())) {
			return false;
		}
		SYNCING.set(Boolean.TRUE);
		return true;
	}

	private static void end() {
		SYNCING.set(Boolean.FALSE);
	}

	public static boolean isSyncing() {
		return Boolean.TRUE.equals(SYNCING.get());
	}

	/** After Witchery blood changes: mirror percentage onto Legends meter. */
	public static void syncLegendsFromWitchery(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) {
			return;
		}
		if (!Loader.isModLoaded("legends") || !Loader.isModLoaded("witchery")) {
			return;
		}
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		if (playerEx == null || !playerEx.isVampire()) {
			return;
		}
		if (!begin()) {
			return;
		}
		try {
			int max = playerEx.getMaxBloodPower();
			int legends;
			if (max <= 0) {
				legends = 0;
			} else {
				legends = MathHelper.clamp_int(
						(int) Math.round(playerEx.getBloodPower() * 100.0D / max), 0, 100);
			}
			PlayerHelper.setBloodMeter(player, legends);
		} finally {
			end();
		}
	}

	/** After Legends blood changes: mirror percentage onto Witchery blood. */
	public static void syncWitcheryFromLegends(EntityPlayer player, int legendsAmount) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) {
			return;
		}
		if (!Loader.isModLoaded("legends") || !Loader.isModLoaded("witchery")) {
			return;
		}
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		if (playerEx == null || !playerEx.isVampire()) {
			return;
		}
		if (!begin()) {
			return;
		}
		try {
			int max = playerEx.getMaxBloodPower();
			int clampedLegends = MathHelper.clamp_int(legendsAmount, 0, 100);
			int witchery;
			if (max <= 0) {
				witchery = 0;
			} else {
				witchery = MathHelper.clamp_int(
						(int) Math.round(clampedLegends * max / 100.0D), 0, max);
			}
			playerEx.setBloodPower(witchery);
		} finally {
			end();
		}
	}
}
