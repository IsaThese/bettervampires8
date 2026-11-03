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
 * Castlevania Dracula Vampiric Bite / Fury Swipes (True Form).
 */
public class LegacyPowerBvVampireBite extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();
	public static final int BITE_SECONDS = 3;
	public static final int FURY_SWIPES_SECONDS = 2;

	@Override
	public String getName() {
		return "bvVampireBite";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return LegendsUtil.isDraculaTrueForm(player) ? "Fury Swipes" : "Vampiric Bite";
	}

	@Override
	public PowerType getType(EntityPlayer player) {
		return PowerType.assignable;
	}

	@Override
	public int getCost(EntityPlayer player) {
		return 30;
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
				LegendsUtil.furySwipes(player, FURY_SWIPES_SECONDS);
			} else {
				LegendsUtil.vampireBite(player, BITE_SECONDS);
			}
			LegacyHelper.setCooldown(player, key, 200);
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
			return "Fury Swipes: ferociously claw the target. Deals ~10 damage + 2 bleeding; "
					+ "fills blood on claw contact.";
		}
		return "Vampiric Bite: bite the victim, cause bleed, and fill your blood meter. "
				+ "Deals ~6 damage and staggers ~5 seconds. Becomes Fury Swipes in True Form.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 7;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 1;
	}
}
