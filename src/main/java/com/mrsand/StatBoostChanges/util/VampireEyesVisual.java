package com.mrsand.StatBoostChanges.util;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.tihyo.legends.management.PlayerHelper;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;


/**
 * BV vampiric-eyes modes and the extra Eye Size texture.
 * <p>
 * {@code VAMPIRE_EYES}: {@code 0} powers-only, {@code 1} always, {@code 2} disabled.
 * {@code EYE_TYPE}: {@code 0} 1x2, {@code 1} 2x2, {@code 2} none, {@code 3} BV 1x2 (offset sheet).
 */
public final class VampireEyesVisual {

	public static final int MODE_POWERS_ONLY = 0;
	public static final int MODE_ALWAYS = 1;
	public static final int MODE_DISABLED = 2;

	public static final int EYE_TYPE_1X2 = 0;
	public static final int EYE_TYPE_2X2 = 1;
	public static final int EYE_TYPE_NONE = 2;
	public static final int EYE_TYPE_BV_1X2 = 3;

	public static final ResourceLocation BV_1X2_EYES =
			new ResourceLocation("bettervampires", "textures/models/legacy/vampire/bv_1x2_eyes.png");

	private VampireEyesVisual() {}

	/**
	 * True while synced {@code HYPNOTIZING} (mesmerize / mastery), hypnosis raise-hand
	 * (Legends {@code AIMING_RIGHT} type 0 — same Replay-stable signal as the arm),
	 * L10 otherform, blood vision, starve OTHERFORM, or vampire-legacy bite channels
	 * (Vampiric Bite, Vampire's Wrath, Blood Lust).
	 * <p>
	 * Do not gate on mastery key duration — that is local-only.
	 */
	public static boolean isPowerEyesActive(EntityPlayer player) {
		if (player == null) return false;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v != null && v.getBoolean(PlayerKeys.HYPNOTIZING)) return true;
		// Align with arm pose for observers / ReplayMod (Legends PlayerDataPacket).
		if (PlayerHelper.isAimingRightArm(player) && PlayerHelper.getRightArmAimingType(player) == 0) {
			return true;
		}
		if (v != null && v.getBoolean(PlayerKeys.OTHERFORM)) return true;
		if (v != null && v.getBoolean(PlayerKeys.IS_STARVING)) return true;
		if (LegendsUtil.bloodVision(player)) return true;
		return isVampireBiteChannelEyes(player);
	}

	/** Vampire legacy: Bite / Wrath / Blood Lust show powers-only eyes for the channel. */
	private static boolean isVampireBiteChannelEyes(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || !LegendsUtil.isVampire(player)) {
			return false;
		}
		return LegendsAbilityBridge.isVampireBite(player)
				|| LegendsAbilityBridge.isVampiresWrath(player)
				|| LegendsAbilityBridge.isBloodLust(player);
	}

	public static int getMode(EntityPlayer player) {
		if (player == null) return MODE_POWERS_ONLY;
		int mode = PlayerHelper.getVampireEyes(player);
		if (mode < MODE_POWERS_ONLY || mode > MODE_DISABLED) return MODE_POWERS_ONLY;
		return mode;
	}

	/** Whether the Legends vampire-eye overlay should draw this frame. */
	public static boolean shouldDrawOverlay(EntityPlayer player) {
		if (player == null) return false;
		int mode = getMode(player);
		if (mode == MODE_DISABLED) return false;
		if (mode == MODE_ALWAYS) return true;
		return isPowerEyesActive(player);
	}

	public static int cycleMode(int current) {
		int next = current + 1;
		if (next > MODE_DISABLED) next = MODE_POWERS_ONLY;
		return next;
	}

	public static int cycleEyeType(int current) {
		int next = current + 1;
		if (next > EYE_TYPE_BV_1X2) next = EYE_TYPE_1X2;
		return next;
	}

	public static String modeChatMessage(int mode) {
		switch (mode) {
		case MODE_ALWAYS:
			return "Vampiric eyes: always shown";
		case MODE_DISABLED:
			return "Vampiric eyes: disabled";
		case MODE_POWERS_ONLY:
		default:
			return "Vampiric eyes: powers only (hypnosis / blood vision / bite / wrath / blood lust / level 10)";
		}
	}

	public static String eyeSizeDescription() {
		return "Cycles through eye sizes to set to player.\n\n1x2\n2x2\nNone\nBV 1x2";
	}
}
