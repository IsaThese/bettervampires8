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
 * Castlevania Dracula Demonic Megiddo.
 */
public class LegacyPowerBvDemonicMegiddo extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();
	public static final int MEGIDDO_SECONDS = 6;

	@Override
	public String getName() {
		return "bvDemonicMegiddo";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return "Demonic Megiddo";
	}

	@Override
	public PowerType getType(EntityPlayer player) {
		// Special ability bar slot (not a normal assignable combat key).
		return PowerType.special;
	}

	@Override
	public int getCost(EntityPlayer player) {
		return 70;
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
			LegendsUtil.demonicMegiddo(player, MEGIDDO_SECONDS);
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
		return "Demonic Megiddo: ultimate chaotic-magic dome. ~80 damage in radius, staggers foes; "
				+ "you are invulnerable during the cast. Does not destroy terrain.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 7;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 2;
	}
}
