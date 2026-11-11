package com.mrsand.StatBoostChanges.util;

import com.emoniph.witchery.brewing.potions.PotionEnslaved;
import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.tihyo.legends.entities.mobs.EntityHerobrine;
import com.tihyo.legends.management.interfaces.IBoss;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Central “should this attacker leave the vampire alone?” policy.
 * <ul>
 *   <li>Normal hostiles: bat-blind, OTHERFORM, fangs/morph/blood vision, lord+Witchery set,
 *       or Vampire Lord nested in a Legends coffin</li>
 *   <li>Bosses / Herobrine: Vampire Lords only (still attack normal vampires)</li>
 * </ul>
 */
public final class VampireHostility {

	private VampireHostility() {}

	/** Normal-mob ignore (zombies, spiders, drowners, most SmartCreatures). */
	public static boolean shouldIgnoreNormal(EntityPlayer player) {
		if (player == null) return false;
		if (GeneralUtil.hasBatBlindStealth(player)) return true;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v != null && v.getBoolean(PlayerKeys.OTHERFORM)) return true;
		if (LegendsUtil.mobAttack(player)) return true;
		if (WitcheryUtil.hasVampireLordClothesCap(player)) return true;
		// Nesting lord: mobs expect loyalty/fear — leave them to rest.
		return DaywalkerControl.isVampireLord(player) && CoffinSleepUtil.isInCoffin(player);
	}

	/** Herobrine, Legends IBoss, vanilla boss display, Kaiju-scale peers. */
	public static boolean isBossOrHerobrine(Entity attacker) {
		if (attacker == null) return false;
		if (attacker instanceof EntityHerobrine) return true;
		if (attacker instanceof IBoss) return true;
		if (attacker instanceof IBossDisplayData) return true;
		return false;
	}

	/**
	 * Full gate for a specific attacker targeting a player.
	 * Honors angry NBT and Witchery enslavement.
	 */
	public static boolean shouldIgnoreAttacker(Entity attacker, EntityPlayer player) {
		if (attacker == null || player == null) return false;
		if (attacker.getEntityData().getBoolean("angry")) return false;
		if (attacker instanceof EntityLiving
				&& PotionEnslaved.isMobEnslavedBy((EntityLiving) attacker, player)) {
			return false;
		}
		if (isBossOrHerobrine(attacker)) {
			return DaywalkerControl.isVampireLord(player);
		}
		return shouldIgnoreNormal(player);
	}

}
