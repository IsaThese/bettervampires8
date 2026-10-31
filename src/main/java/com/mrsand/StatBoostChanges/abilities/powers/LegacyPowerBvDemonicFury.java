package com.mrsand.StatBoostChanges.abilities.powers;

import java.util.List;

import com.google.common.collect.Lists;
import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.legacy.LegendsLegacy;
import com.tihyo.legends.legacy.PowerType;
import com.tihyo.legends.management.LegendsChatFormat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

public class LegacyPowerBvDemonicFury extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();

	@Override
	public String getName() {
		return "bvDemonicFury";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return "Demonic Fury";
	}

	@Override
	public PowerType getType(EntityPlayer player) {
		return PowerType.assignable;
	}

	@Override
	public int getCost(EntityPlayer player) {
		return 40;
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
			VampireRestoredAbilities.activateDemonicFury(player, VampireRestoredAbilities.DEFAULT_FURY_SECONDS);
			LegacyHelper.setCooldown(player, key, 400);
			player.addChatMessage(new ChatComponentText(LegendsChatFormat.red + "Demonic fury surges through you."));
		}
	}

	@Override
	public void execute(EntityPlayer player) {
		// Timed attack buff; no hold behavior.
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
		return "Enter a fury: attacks drain blood and grant combat rewards for a short time.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 3;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 1;
	}
}
