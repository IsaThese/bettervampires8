package de.teamlapen.vampirism.entity.player.skills;

import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import cpw.mods.fml.common.Loader;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Lord mist form (replaces vanilla invisibility). Uses Legends mistForm.
 * Indefinite while active; second pie press starts a 2s demist delay (not instant).
 * Start blocked in open sun / open daytime for lords; pie stays while misted so demist works anytime.
 */
public class InvisibilitySkill extends DefaultSkill implements ILastingSkill {

	@Override
	public boolean canBeUsedBy(VampirePlayer vampire, EntityPlayer player) {
		// Keep available while misted so pie can start/cancel the 2s demist
		return Loader.isModLoaded("legends")
				&& vampire.isVampireLord()
				&& !player.isInWater();
	}

	/**
	 * Show in pie while misted (even in open day/sun) for demist.
	 * Start only when {@link VampireSunRules#canStartMist}.
	 */
	@Override
	public int canUse(VampirePlayer vampire, EntityPlayer player) {
		if (getMinLevel() == -1)
			return -1;
		if (vampire.getLevel() < getMinLevel())
			return 0;
		if (!canBeUsedBy(vampire, player))
			return -2;
		boolean misted = vampire.isSkillActive(this) || LegendsUtil.isMist(player);
		if (misted)
			return 1;
		if (player == null || player.worldObj == null)
			return -2;
		if (!VampireSunRules.canStartMist(player))
			return -2;
		return 1;
	}

	@Override
	public int getCooldown() {
		// Match Legends mist key OOC: ready immediately after demist
		if (Loader.isModLoaded("legends")) {
			return 0;
		}
		return BALANCE.VP_SKILLS.INVISIBILITY_COOLDOWN * 20;
	}

	@Override
	public int getDuration(int level) {
		if (Loader.isModLoaded("legends")) {
			return Integer.MAX_VALUE;
		}
		return BALANCE.VP_SKILLS.INVISIBILITY_DURATION * 20;
	}

	@Override
	public int getMinLevel() {
		return 0;
	}

	@Override
	public int getMinU() {
		return 128;
	}

	@Override
	public int getMinV() {
		return 0;
	}

	@Override
	public String getUnlocalizedName() {
		return "skill.vampirism.invisibility";
	}

	@Override
	public boolean onActivated(VampirePlayer vampire, EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote || !Loader.isModLoaded("legends")) {
			return false;
		}
		if (!VampireSunRules.canStartMist(player)) {
			return false;
		}
		LegendsUtil.ifMorphedTurnHuman(player);
		if (!WitcheryUtil.tryPayMistCombatBlood(player)) {
			return false;
		}
		LegendsUtil.activateMistFormIndefinite(player);
		return true;
	}

	/**
	 * Called instead of instant deactivate when pie-toggled while mist is active.
	 * Starts 2s demist, or cancels demist if already solidifying (stay misted).
	 */
	public void requestExit(VampirePlayer vampire, EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote || !Loader.isModLoaded("legends")) return;
		LegendsUtil.requestMistDemist(player);
	}

	@Override
	public void onDeactivated(VampirePlayer vampire, EntityPlayer player) {
		LegendsUtil.clearMistForm(player);
	}

	@Override
	public void onReActivated(VampirePlayer vampire, EntityPlayer player) {
		if (player != null && player.worldObj != null && !player.worldObj.isRemote && Loader.isModLoaded("legends")
				&& !LegendsUtil.isMist(player) && VampireSunRules.canStartMist(player)) {
			LegendsUtil.activateMistFormIndefinite(player);
		}
	}

	@Override
	public boolean onUpdate(VampirePlayer vampire, EntityPlayer player) {
		if (!Loader.isModLoaded("legends")) return true;
		// Non-lords: force demist in sunlight. Lords keep mist until they exit.
		if (VampireSunRules.shouldForceClearMist(player)) {
			return true;
		}
		// Demist countdown is ticked globally; end skill once mist is gone
		return !LegendsUtil.isMist(player);
	}

}
