package de.teamlapen.vampirism.entity.player.skills;

import de.teamlapen.vampirism.entity.EntityBlindingBat;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import de.teamlapen.vampirism.util.REFERENCE;

import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Disengage: become Legends bat (or stay bat if already), spawn blinding bats, clear nearby aggro.
 */
public class SummonBatSkill extends DefaultSkill {

	private static final int DISENGAGE_RADIUS = 24;
	private static final int FORGET_TICKS = 80;

	@Override
	public boolean canBeUsedBy(VampirePlayer vampire, EntityPlayer player) {
		return Loader.isModLoaded("legends") && !player.isInWater()
				&& !VampireSunRules.generalPowersBlocked(player);
	}

	@Override
	public int getCooldown() {
		return BALANCE.VP_SKILLS.SUMMON_BAT_COOLDOWN * 20;
	}

	@Override
	public int getMinLevel() {
		return BALANCE.VP_SKILLS.SUMMON_BAT_MIN_LEVEL;
	}

	@Override
	public int getMinU() {
		return 96;
	}

	@Override
	public int getMinV() {
		return 0;
	}

	@Override
	public String getUnlocalizedName() {
		return "skill.vampirism.summon_bats";
	}

	@Override
	public boolean onActivated(VampirePlayer vampire, EntityPlayer player) {
		if (player.worldObj.isRemote || !Loader.isModLoaded("legends")) {
			return false;
		}

		// Morph to bat, or keep current bat form
		if (!LegendsUtil.tryForceBatMorph(player)) {
			return false;
		}

		for (int i = 0; i < BALANCE.VP_SKILLS.SUMMON_BAT_COUNT; i++) {
			Entity e = EntityList.createEntityByName(REFERENCE.ENTITY.BLINDING_BAT_NAME, player.worldObj);
			((EntityBlindingBat) e).restrictLiveSpan();
			e.copyLocationAndAnglesFrom(player);
			player.worldObj.spawnEntityInWorld(e);
		}

		GeneralUtil.disengageFromMobs(player, DISENGAGE_RADIUS, FORGET_TICKS);
		return true;
	}

}
