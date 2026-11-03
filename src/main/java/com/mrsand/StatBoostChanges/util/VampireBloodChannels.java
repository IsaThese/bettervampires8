package com.mrsand.StatBoostChanges.util;

import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.properties.LegendsProperties;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Retunes Legends blood-channel abilities for BV:
 * <ul>
 *   <li>Vampiric Drain ({@code BLOOD_DRAIN}) — same +5 / 10t as Vampiric Bite</li>
 *   <li>Vampire's Wrath ({@code VAMPIRES_WRATH}) — ≥30% blood on successful contact</li>
 * </ul>
 */
public final class VampireBloodChannels {

	public static final int BITE_PULSE_AMOUNT = 5;
	public static final int BITE_PULSE_INTERVAL = 10;
	public static final double DRAIN_RANGE = 2.0D;
	public static final double WRATH_RANGE = 3.0D;
	public static final int WRATH_MIN_BLOOD = 30;

	private static final String TAG_WRATH_TRACKING = "bvWrathTracking";
	private static final String TAG_WRATH_GAINED = "bvWrathGained";
	private static final String TAG_WRATH_CONTACT = "bvWrathContact";
	private static final String TAG_WRATH_LAST_BLOOD = "bvWrathLastBlood";
	/** Suppress stock flat +20 during drain so only bite-rate pulses apply. */
	private static final ThreadLocal<Boolean> SUPPRESS_DRAIN_FLAT = new ThreadLocal<Boolean>();

	private VampireBloodChannels() {}

	public static boolean shouldSuppressDrainFlatGrant(EntityPlayer player, int amount) {
		if (amount != 20 || player == null) return false;
		if (Boolean.TRUE.equals(SUPPRESS_DRAIN_FLAT.get())) return false;
		return LegendsAbilityBridge.getIntProp(player, LegendsProperties.BLOOD_DRAIN) > 0;
	}

	/** Server player tick (Phase.END preferred so Horror wrath pulses are visible). */
	public static void tick(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj == null
				|| player.worldObj.isRemote) {
			return;
		}
		tickBloodDrain(player);
		tickVampiresWrath(player);
	}

	private static void tickBloodDrain(EntityPlayer player) {
		if (LegendsAbilityBridge.getIntProp(player, LegendsProperties.BLOOD_DRAIN) <= 0) {
			return;
		}
		if (LegendsHelper.getTicks(player) % BITE_PULSE_INTERVAL != 0) {
			return;
		}
		EntityLivingBase target = LegendsHelper.getTarget(player, DRAIN_RANGE);
		if (!isValidBloodContact(player, target)) {
			return;
		}
		SUPPRESS_DRAIN_FLAT.set(Boolean.TRUE);
		try {
			PlayerHelper.increaseBloodMeter(player, BITE_PULSE_AMOUNT, false);
		} finally {
			SUPPRESS_DRAIN_FLAT.set(Boolean.FALSE);
		}
	}

	private static void tickVampiresWrath(EntityPlayer player) {
		int wrath = LegendsAbilityBridge.getIntProp(player, LegendsProperties.VAMPIRES_WRATH);
		NBTTagCompound data = player.getEntityData();
		boolean tracking = data.getBoolean(TAG_WRATH_TRACKING);

		if (wrath > 0) {
			if (!tracking) {
				data.setBoolean(TAG_WRATH_TRACKING, true);
				data.setInteger(TAG_WRATH_GAINED, 0);
				data.setBoolean(TAG_WRATH_CONTACT, false);
				data.setInteger(TAG_WRATH_LAST_BLOOD, PlayerHelper.getBloodAmount(player));
			}
			EntityLivingBase target = LegendsHelper.getTarget(player, WRATH_RANGE);
			if (isValidBloodContact(player, target)) {
				data.setBoolean(TAG_WRATH_CONTACT, true);
			}
			int blood = PlayerHelper.getBloodAmount(player);
			int last = data.getInteger(TAG_WRATH_LAST_BLOOD);
			if (blood > last) {
				data.setInteger(TAG_WRATH_GAINED, data.getInteger(TAG_WRATH_GAINED) + (blood - last));
			}
			data.setInteger(TAG_WRATH_LAST_BLOOD, blood);
			return;
		}

		if (!tracking) {
			return;
		}

		boolean hadContact = data.getBoolean(TAG_WRATH_CONTACT);
		int gained = data.getInteger(TAG_WRATH_GAINED);
		data.removeTag(TAG_WRATH_TRACKING);
		data.removeTag(TAG_WRATH_GAINED);
		data.removeTag(TAG_WRATH_CONTACT);
		data.removeTag(TAG_WRATH_LAST_BLOOD);

		if (hadContact && gained < WRATH_MIN_BLOOD) {
			PlayerHelper.increaseBloodMeter(player, WRATH_MIN_BLOOD - gained, false);
		}
	}

	private static boolean isValidBloodContact(EntityPlayer player, EntityLivingBase target) {
		if (target == null || !(target instanceof EntityLivingBase)) {
			return false;
		}
		if (!LegendsHelper.isBloodTarget(target)) {
			return false;
		}
		if (!PlayerHelper.canBeTouched(player, target)) {
			return false;
		}
		if (LegendsHelper.isAllyEntity(player, target)) {
			return false;
		}
		if (target instanceof EntityPlayer) {
			EntityPlayer other = (EntityPlayer) target;
			if (com.tihyo.legends.guilds.GuildHelper.isInSameGuild(player, other)
					|| com.tihyo.legends.guilds.GuildHelper.isAllied(player, other)) {
				return false;
			}
		}
		return true;
	}
}
