package com.mrsand.StatBoostChanges.util;

import com.tihyo.legends.abilities.Stat;
import com.tihyo.legends.abilities.StatHelper;
import com.tihyo.legends.abilities.StatusEffect;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Thin adapter for Legends 8: vampire status moved from AbilityHelper/Ability.*
 * to LegendsProperties + StatHelper.
 */
public final class LegendsAbilityBridge {

	private LegendsAbilityBridge() {}

	public static int getIntProp(EntityLivingBase entity, LegendsProperties<Integer> prop) {
		if (entity == null || prop == null) return 0;
		Object v = prop.get(entity);
		return v instanceof Integer ? ((Integer) v).intValue() : 0;
	}

	public static boolean getBoolProp(EntityLivingBase entity, LegendsProperties<Boolean> prop) {
		if (entity == null || prop == null) return false;
		Object v = prop.get(entity);
		return v instanceof Boolean && ((Boolean) v).booleanValue();
	}

	public static boolean isMist(EntityLivingBase entity) {
		return getIntProp(entity, LegendsProperties.MIST_FORM) > 0;
	}

	public static boolean isBloodLust(EntityLivingBase entity) {
		return getIntProp(entity, LegendsProperties.BLOOD_LUST) > 0;
	}

	public static boolean isVampireFangs(EntityLivingBase entity) {
		return getBoolProp(entity, LegendsProperties.VAMPIRE_FANGS);
	}

	public static boolean isBloodVision(EntityLivingBase entity) {
		return getBoolProp(entity, LegendsProperties.BLOOD_VISION);
	}

	public static boolean isVampireBite(EntityLivingBase entity) {
		return getIntProp(entity, LegendsProperties.VAMPIRE_BITE) > 0;
	}

	public static boolean isFury(EntityLivingBase entity) {
		return getIntProp(entity, LegendsProperties.FURY) > 0;
	}

	public static boolean isHypnosis(EntityLivingBase entity) {
		return getIntProp(entity, LegendsProperties.HYPNOSIS) > 0;
	}

	public static boolean isInvincible(EntityLivingBase entity) {
		return getIntProp(entity, LegendsProperties.INVINCIBLE) > 0
				|| StatHelper.isAbilityActive(entity, Stat.invulnerability);
	}

	public static boolean hasBloodMeter(EntityPlayer player) {
		return getIntProp(player, LegendsProperties.BLOOD_AMOUNT) > 0;
	}

	public static void setMistForm(EntityPlayer player, int durationSeconds) {
		if (player == null) return;
		PowersHandler.mistForm(player, durationSeconds);
	}

	public static void clearMistForm(EntityPlayer player) {
		if (player == null) return;
		LegendsProperties.MIST_FORM.set(player, Integer.valueOf(0));
	}

	public static void setMistFormIndefinite(EntityPlayer player) {
		if (player == null) return;
		// Large tick count; Legends stores seconds * 20 inside PowersHandler.mistForm
		LegendsProperties.MIST_FORM.set(player, Integer.valueOf(Integer.MAX_VALUE / 4));
	}

	public static void clearBloodLust(EntityPlayer player) {
		if (player == null) return;
		LegendsProperties.BLOOD_LUST.set(player, Integer.valueOf(0));
	}

	public static void setBloodVision(EntityPlayer player, boolean on) {
		if (player == null) return;
		PowersHandler.bloodVision(player, on);
	}

	public static void setInvincibleTicks(EntityPlayer player, int ticks, int amplifier) {
		if (player == null) return;
		LegendsProperties.INVINCIBLE.set(player, Integer.valueOf(ticks));
		StatHelper.addEffectAndSync(player, Stat.invulnerability, ticks, amplifier);
	}

	public static boolean isStatActive(EntityLivingBase entity, Stat stat) {
		return entity != null && stat != null && StatHelper.isAbilityActive(entity, stat);
	}

	public static StatusEffect getStatEffect(EntityLivingBase entity, Stat stat) {
		return StatHelper.getEffect(entity, stat);
	}
}
