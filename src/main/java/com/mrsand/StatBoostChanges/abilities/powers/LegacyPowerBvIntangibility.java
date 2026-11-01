package com.mrsand.StatBoostChanges.abilities.powers;

import java.util.List;

import com.google.common.collect.Lists;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.legacy.LegendsLegacy;
import com.tihyo.legends.legacy.PowerType;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Lord-only passive: pass through cobwebs. Shown on the vampire tree; not purchasable.
 * Ownership is forced via {@code LegacyHelperMixin} when the player is a Vampirism lord.
 */
public class LegacyPowerBvIntangibility extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();

	@Override
	public String getName() {
		return "bvIntangibility";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return "Intangibility";
	}

	@Override
	public PowerType getType(EntityPlayer player) {
		return PowerType.defaultPassiveAbility;
	}

	@Override
	public int getCost(EntityPlayer player) {
		return 0;
	}

	@Override
	public int getAssignedKey(EntityPlayer player) {
		return 0;
	}

	@Override
	public void keyPressed(EntityPlayer player) {
		// Passive — no activation.
	}

	@Override
	public void execute(EntityPlayer player) {
		// Passive — no hold behavior.
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
		return LegacyRegistry.fangs;
	}

	@Override
	public String getDescription(EntityPlayer player) {
		return "Vampire lords pass through cobwebs unhindered.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 1;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return -1;
	}
}
