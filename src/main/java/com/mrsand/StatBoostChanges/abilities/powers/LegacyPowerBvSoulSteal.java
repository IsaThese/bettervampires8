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
 * Castlevania Dracula Soul Steal.
 */
public class LegacyPowerBvSoulSteal extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();
	public static final int SOUL_STEAL_SECONDS = 2;

	@Override
	public String getName() {
		return "bvSoulSteal";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return "Soul Steal";
	}

	@Override
	public PowerType getType(EntityPlayer player) {
		return PowerType.assignable;
	}

	@Override
	public int getCost(EntityPlayer player) {
		return 45;
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
			LegendsUtil.soulSteal(player, SOUL_STEAL_SECONDS);
			LegacyHelper.setCooldown(player, key, 500);
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
		return "Soul Steal: drain nearby foes' souls. Deals ~5 damage twice and heals ~10 "
				+ "(up to ~20 with more targets). Chance to add essence to the Soul Well.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 6;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 2;
	}
}
