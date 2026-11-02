package com.mrsand.StatBoostChanges.abilities.powers;

import java.util.List;

import com.google.common.collect.Lists;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.legacy.LegendsLegacy;
import com.tihyo.legends.legacy.PowerType;
import com.tihyo.legends.management.LegendsChatFormat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

public class LegacyPowerBvDemonicForm extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();

	@Override
	public String getName() {
		return "bvDemonicForm";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return "Demonic Form";
	}

	@Override
	public PowerType getType(EntityPlayer player) {
		return PowerType.assignable;
	}

	@Override
	public int getCost(EntityPlayer player) {
		return 60;
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
		if (player.isInWater()) {
			warn(player, "Can't transform in water.");
			return;
		}
		if (!player.worldObj.isRemote) {
			LegendsUtil.morphBeast(player);
			// No post-morph cooldown — leave key at cooldownMax (ready).
		}
	}

	@Override
	public void execute(EntityPlayer player) {
		// Activation is keyPressed (toggle morph).
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
		return "Transform into a demonic beast. Raises suspicion if witnessed.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 2;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 1;
	}

	private void warn(EntityPlayer player, String msg) {
		if (!player.worldObj.isRemote) {
			player.addChatMessage(new ChatComponentText(LegendsChatFormat.red + msg));
		}
	}
}
