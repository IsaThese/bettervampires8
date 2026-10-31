package com.mrsand.StatBoostChanges.abilities.powers;

import java.util.List;

import com.google.common.collect.Lists;
import com.mrsand.StatBoostChanges.abilities.HypnosisMasteryLogic;
import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.legacy.LegendsLegacy;
import com.tihyo.legends.legacy.PowerType;
import com.tihyo.legends.management.LegendsChatFormat;
import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.morph.MorphHelper;

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

	/** Required in Legends 8: base canHold returns false, so onHold never runs. */
	@Override
	public boolean canHold(EntityPlayer player) {
		return true;
	}

	@Override
	public void keyPressed(EntityPlayer player) {
		// Continuous channel: onHold refreshes duration; execute runs while useAbility is true.
	}

	/**
	 * Mirror Legends 5.4 Hypnosis Mastery: while held, keep ability duration &gt; 0
	 * so {@link LegacyHelper#useAbility} passes in execute.
	 */
	@Override
	public void onHold(EntityPlayer player) {
		if (player == null) return;
		int key = getAssignedKey(player);
		if (key <= 0) return;

		if (MorphHelper.isMorphed(player)) {
			if (LegendsHelper.getTicks(player) % 10 == 0) {
				warn(player, "Must return to human form to hypnotize.");
			}
			return;
		}

		if (VampireRestoredAbilities.getHypnosisStrength(player) <= 0) return;
		if (LegacyHelper.getLegacyClass(player) == null) return;
		if (LegacyHelper.getLegacyClass(player).getWeaknesses(player)) return;

		LegacyHelper.setDuration(player, key, 4);
		// Legends 5: client must raycast + sync CURRENT_TARGET_ID while holding.
		HypnosisMasteryLogic.refreshClientTarget(player);
	}

	@Override
	public void execute(EntityPlayer player) {
		if (player == null) return;
		int key = getAssignedKey(player);
		if (key <= 0) return;
		// Client: still tick so getTarget can sync the crosshair entity to the server.
		if (player.worldObj.isRemote) {
			HypnosisMasteryLogic.tickChannel(player);
			return;
		}
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
		return "Hold to hypnotize. Stand still to stun a target and, after ~6s of focus, enthrall / tame / paralyse them (blood cost; one success message). Sneak to drag them toward the block behind them along your gaze without binding. The purple bar does not drain. Punch applies standard hypnosis.";
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
