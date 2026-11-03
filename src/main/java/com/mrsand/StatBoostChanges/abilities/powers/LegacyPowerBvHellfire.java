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
 * Castlevania Dracula Hellfire / Wave of Fireballs (True Form).
 */
public class LegacyPowerBvHellfire extends LegacyPower {

	private static final List<LegendsLegacy> legacies = Lists.newArrayList();
	public static final int HELLFIRE_SECONDS = 1;
	public static final int WAVE_SECONDS = 2;

	@Override
	public String getName() {
		return "bvHellfire";
	}

	@Override
	public String getDisplayName(EntityPlayer player) {
		return LegendsUtil.isDraculaTrueForm(player) ? "Wave of Fireballs" : "Hellfire";
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
			if (LegendsUtil.isDraculaTrueForm(player)) {
				LegendsUtil.waveOfFireballs(player, WAVE_SECONDS);
			} else {
				LegendsUtil.hellfire(player, HELLFIRE_SECONDS);
			}
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
		if (LegendsUtil.isDraculaTrueForm(player)) {
			return "Wave of Fireballs: expel three large fireballs. Each deals ~5 damage and staggers.";
		}
		return "Hellfire: call forth three hellfireballs. Deals ~15 damage + 2 burn. "
				+ "Becomes Wave of Fireballs in True Form.";
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
