package com.mrsand.StatBoostChanges.abilities.powers;

import java.util.List;

import com.google.common.collect.Lists;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.legacy.LegendsLegacy;
import com.tihyo.legends.legacy.PowerType;

import net.minecraft.entity.player.EntityPlayer;

/**
 * The Count's Vampire's Wrath — special embrace channel that drains life and blood.
 */
public class LegacyPowerBvVampiresWrath extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();
	public static final int WRATH_SECONDS = 5;

	@Override
	public String getName() {
		return "bvVampiresWrath";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return "Vampire's Wrath";
	}

	@Override
	public PowerType getType(EntityPlayer player) {
		return PowerType.special;
	}

	@Override
	public int getCost(EntityPlayer player) {
		return 100;
	}

	@Override
	public int getAssignedKey(EntityPlayer player) {
		return LegacyHelper.findAbility(player, getName());
	}

	@Override
	public void keyPressed(EntityPlayer player) {
		int key = getAssignedKey(player);
		if (key <= 0) return;
		if (LegacyHelper.getCooldown(player, key) != LegacyHelper.getCooldownMax(player, key)) return;
		if (LegacyHelper.getLegacyClass(player) == null
				|| LegacyHelper.getLegacyClass(player).getWeaknesses(player)) {
			return;
		}
		if (!player.worldObj.isRemote) {
			LegendsUtil.vampiresWrath(player, WRATH_SECONDS);
			LegacyHelper.setCooldown(player, key, 1000);
		}
	}

	@Override
	public void execute(EntityPlayer player) {
	}

	@Override
	public void addToLegacy() {
		LegacyHelper.addPowerToLegacy(this, legacies, LegacyRegistry.vampire);
	}

	@Override
	public List<LegendsLegacy> getLegacies() {
		return legacies;
	}

	@Override
	public LegacyPower getParent(EntityPlayer player) {
		return BvVampirePowers.trueForm;
	}

	@Override
	public String getDescription(EntityPlayer player) {
		return "Vampire's Wrath: embrace your victim, draining life and blood. "
				+ "Deals heavy damage while channeling; grants at least 30% blood on a successful hold. "
				+ "Same ability as The Count.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 8;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 2;
	}
}
