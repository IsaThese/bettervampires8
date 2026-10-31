package com.mrsand.StatBoostChanges.abilities.powers;

import java.util.List;

import com.google.common.collect.Lists;
import com.mrsand.StatBoostChanges.abilities.HypnosisMasteryLogic;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.legacy.LegendsLegacy;
import com.tihyo.legends.legacy.PowerType;
import com.tihyo.legends.management.LegendsChatFormat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

public class LegacyPowerBvHypnosisMastery extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();

	@Override
	public String getName() {
		return "bvHypnosisMastery";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return "Hypnosis Mastery";
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
		// Continuous channel runs in execute while the assigned key is held.
	}

	@Override
	public void execute(EntityPlayer player) {
		if (player == null) return;
		int key = getAssignedKey(player);
		if (key <= 0) return;
		if (!LegacyHelper.useAbility(player, key)) return;
		if (LegacyHelper.getLegacyClass(player) == null) return;
		if (LegacyHelper.getLegacyClass(player).getWeaknesses(player)) return;
		HypnosisMasteryLogic.tickChannel(player);
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
		return LegacyRegistry.hypnosis;
	}

	@Override
	public String getDescription(EntityPlayer player) {
		return "Mastered hypnosis: channel to paralyze villagers, thrall creatures, and dominate tameables.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 2;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 0;
	}

	protected void warn(EntityPlayer player, String msg) {
		if (!player.worldObj.isRemote) {
			player.addChatMessage(new ChatComponentText(LegendsChatFormat.red + msg));
		}
	}
}
