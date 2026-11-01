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
 * Castlevania Dracula teleport ({@code PowersHandler.draculaTeleport}).
 */
public class LegacyPowerBvDraculaTeleport extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();
	/** Match Castlevania suit ability duration (seconds). */
	public static final int TELEPORT_SECONDS = 11;

	@Override
	public String getName() {
		return "bvDraculaTeleport";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return "Castlevania Teleport";
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
			LegendsUtil.draculaTeleport(player, TELEPORT_SECONDS);
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
		return "Castlevania Dracula's teleport — vanish and reappear after a short delay.";
	}

	@Override
	public int getXOffset(EntityPlayer player) {
		return 5;
	}

	@Override
	public int getYOffset(EntityPlayer player) {
		return 1;
	}
}
