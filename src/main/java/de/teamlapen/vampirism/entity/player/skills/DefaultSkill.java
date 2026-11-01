package de.teamlapen.vampirism.entity.player.skills;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;
import com.mrsand.StatBoostChanges.abilities.VampireSunRules;

import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.REFERENCE;

/**
 * Default implementation of ISkill, provides some standard methods
 * 
 * @author maxanier
 *
 */
public abstract class DefaultSkill implements ISkill {
	private final static ResourceLocation defaultIcons = new ResourceLocation(REFERENCE.MODID + ":textures/gui/skills.png");
	private int id;

	/**
	 * Can be overriden to check addidional requirements
	 * 
	 * @param vampire
	 * @param player
	 * @return
	 */
	public boolean canBeUsedBy(VampirePlayer vampire, EntityPlayer player) {
		return true;
	}

	@Override
	public int canUse(VampirePlayer vampire, EntityPlayer player) {
		if (getMinLevel() == -1)
			return -1;
		if (vampire == null || player == null || player.worldObj == null)
			return -2;
		if (vampire.getLevel() < getMinLevel())
			return 0;
		// Cancel/FakeSkill id is -1 — skip sun/day gates
		if (getId() >= 0) {
			boolean lord = false;
			try {
				lord = DaywalkerControl.isVampireLord(player);
			} catch (Throwable ignored) {
				lord = vampire.isVampireLord();
			}
			if (lord && VampireSunRules.generalPowersBlocked(player)) {
				// Daytime lords: only weather + mist (those subclasses override / canBeUsedBy)
				if (!(this instanceof ChangeWeatherSkill) && !(this instanceof InvisibilitySkill)) {
					return -2;
				}
			} else if (!lord && VampireSunRules.isInSunlight(player)) {
				return -2;
			}
		}
		return (canBeUsedBy(vampire, player) ? 1 : -2);
	}

	@Override
	public ResourceLocation getIconLoc() {
		return defaultIcons;
	}

	@Override
	public int getId() {
		return this.id;
	}

	/**
	 * @return The minimum level which is required to use this skill
	 */
	public abstract int getMinLevel();

	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + ":" + id;
	}

}
