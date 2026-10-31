package de.teamlapen.vampirism.entity.player.skills;

import com.mrsand.StatBoostChanges.util.LegendsUtil;

import cpw.mods.fml.common.Loader;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * Vampire Rage — uses Legends Blood Lust when Legends is loaded.
 * Lasting skill state drives the red HUD border.
 */
public class VampireRageSkill extends DefaultSkill implements ILastingSkill {

	@Override
	public boolean canBeUsedBy(VampirePlayer vampire, EntityPlayer player) {
		if (vampire.isSkillActive(Skills.batMode)) {
			return false;
		}
		if (Loader.isModLoaded("legends")) {
			return !LegendsUtil.isMist(player) && !LegendsUtil.isBloodLust(player);
		}
		return true;
	}

	@Override
	public int getCooldown() {
		return BALANCE.VP_SKILLS.RAGE_COOLDOWN * 20;
	}

	@Override
	public int getDuration(int level) {
		return BALANCE.VP_SKILLS.getVampireLordDuration(level);
	}

	@Override
	public int getMinLevel() {
		return BALANCE.VP_SKILLS.RAGE_MIN_LEVEL;
	}

	@Override
	public int getMinU() {
		return 32;
	}

	@Override
	public int getMinV() {
		return 0;
	}

	@Override
	public String getUnlocalizedName() {
		return "skill.vampirism.vampire_rage";
	}

	@Override
	public boolean onActivated(VampirePlayer vampire, EntityPlayer player) {
		int durationTicks = getDuration(vampire.getLevel());
		if (durationTicks <= 0) {
			return false;
		}
		if (!player.worldObj.isRemote && Loader.isModLoaded("legends")) {
			int seconds = Math.max(1, durationTicks / 20);
			LegendsUtil.activateBloodLust(player, seconds);
		} else {
			player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, durationTicks, 2));
		}
		return true;
	}

	@Override
	public void onDeactivated(VampirePlayer vampire, EntityPlayer player) {
		if (Loader.isModLoaded("legends")) {
			LegendsUtil.clearBloodLust(player);
		}
		player.removePotionEffect(Potion.moveSpeed.id);
	}

	@Override
	public void onReActivated(VampirePlayer vampire, EntityPlayer player) {
		if (player.worldObj.isRemote) return;
		int durationTicks = getDuration(vampire.getLevel());
		if (Loader.isModLoaded("legends")) {
			if (!LegendsUtil.isBloodLust(player)) {
				LegendsUtil.activateBloodLust(player, Math.max(1, durationTicks / 20));
			}
		} else if (!player.isPotionActive(Potion.moveSpeed)) {
			player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, durationTicks, 2));
		}
	}

	@Override
	public boolean onUpdate(VampirePlayer vampire, EntityPlayer player) {
		if (vampire.isInSunlight()) {
			return true;
		}
		if (Loader.isModLoaded("legends") && !LegendsUtil.isBloodLust(player)) {
			return true;
		}
		return false;
	}

}
