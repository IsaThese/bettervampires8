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
 * Castlevania Dracula Battle Precognition / Combat (True Form).
 */
public class LegacyPowerBvBattlePrecognition extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();
	public static final int PRECOG_SECONDS = 10;
	public static final int COMBAT_SECONDS = 15;

	@Override
	public String getName() {
		return "bvBattlePrecognition";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return LegendsUtil.isDraculaTrueForm(player) ? "Combat" : "Battle Precognition";
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
			if (LegendsUtil.isDraculaTrueForm(player)) {
				LegendsUtil.combat(player, COMBAT_SECONDS);
			} else {
				LegendsUtil.battlePrecognition(player, PRECOG_SECONDS);
			}
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
		if (LegendsUtil.isDraculaTrueForm(player)) {
			return "Combat: centuries of melee mastery for ~15s. Damage ramps every few hits; "
					+ "can void basic melee; reduced enemy knockback after winded.";
		}
		return "Battle Precognition: anticipate attacks for ~10s — voids most damage and "
				+ "ignores Distortion. Becomes Combat in True Form.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 4;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 2;
	}
}
