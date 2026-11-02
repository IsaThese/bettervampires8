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
 * Castlevania Dracula Hellfire ({@code PowersHandler.hellfire}) — same volley as the suit ability.
 */
public class LegacyPowerBvHellfire extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();
	/** Match Castlevania suit ability2 duration (seconds fed to PowersHandler). */
	public static final int HELLFIRE_SECONDS = 1;

	@Override
	public String getName() {
		return "bvHellfire";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return "Hellfire";
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
			LegendsUtil.hellfire(player, HELLFIRE_SECONDS);
			LegacyHelper.setCooldown(player, key, 200);
		}
	}

	@Override
	public void execute(EntityPlayer player) {
		// Instant cast on keyPressed.
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
		return "Castlevania Dracula's Hellfire — launch a torrent of magical flame.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 6;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 1;
	}
}
