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
 * Castlevania Dracula Dark Inferno / Rampage (True Form).
 */
public class LegacyPowerBvDarkInferno extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();
	public static final int INFERNO_SECONDS = 3;
	public static final int RAMPAGE_SECONDS = 5;

	@Override
	public String getName() {
		return "bvDarkInferno";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return LegendsUtil.isDraculaTrueForm(player) ? "Rampage" : "Dark Inferno";
	}

	@Override
	public PowerType getType(EntityPlayer player) {
		return PowerType.assignable;
	}

	@Override
	public int getCost(EntityPlayer player) {
		return 50;
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
				LegendsUtil.rampage(player, RAMPAGE_SECONDS);
			} else {
				LegendsUtil.darkInferno(player, INFERNO_SECONDS);
			}
			LegacyHelper.setCooldown(player, key, 400);
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
			return "Rampage: charge ~5s, voiding damage and knocking foes aside. "
					+ "~5 damage per collision.";
		}
		return "Dark Inferno: torrent of dark magical power (Hellfire variant). "
				+ "~10 damage up close (often ~20 total); stuns ~3s. Becomes Rampage in True Form.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 5;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 2;
	}
}
