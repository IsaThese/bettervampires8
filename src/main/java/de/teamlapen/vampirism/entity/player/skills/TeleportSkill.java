package de.teamlapen.vampirism.entity.player.skills;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.mrsand.StatBoostChanges.abilities.powers.LegacyPowerBvDraculaTeleport;
import com.mrsand.StatBoostChanges.util.LegendsUtil;

import cpw.mods.fml.common.Loader;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import de.teamlapen.vampirism.util.Helper;
import de.teamlapen.vampirism.util.Logger;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MovingObjectPosition;

public class TeleportSkill extends DefaultSkill {

	@Override
	public boolean canBeUsedBy(VampirePlayer vampire, EntityPlayer player) {
		return vampire.isVampireLord() && !VampireSunRules.generalPowersBlocked(player);
	}

	@Override
	public int getCooldown() {
		return 2;
	}

	@Override
	public int getMinLevel() {
		return BALANCE.VP_SKILLS.TELEPORT_MIN_LEVEL;
	}

	@Override
	public int getMinU() {
		return 112;
	}

	@Override
	public int getMinV() {
		return 0;
	}

	@Override
	public String getUnlocalizedName() {
		return "skill.vampirism.teleport";
	}

	@Override
	public boolean onActivated(VampirePlayer vampire, EntityPlayer player) {
		if (StatBoostConfig.useCastlevaniaDraculaTeleport
				&& Loader.isModLoaded("legends")
				&& !player.worldObj.isRemote) {
			LegendsUtil.draculaTeleport(player, LegacyPowerBvDraculaTeleport.TELEPORT_SECONDS);
			return true;
		}
		return activateRaycastBlink(player);
	}

	/** Original Vampirism short blink to look-at block. */
	private static boolean activateRaycastBlink(EntityPlayer player) {
		MovingObjectPosition pos = Helper.getPlayerLookingSpot(player, BALANCE.VP_SKILLS.TELEPORT_MAX_DISTANCE);
		double ox = player.posX;
		double oy = player.posY;
		double oz = player.posZ;
		if (pos == null) {
			player.worldObj.playSoundAtEntity(player, "note.bass", 1.0F, 1.0F);
			return false;
		}
		int x = pos.blockX;
		int y = pos.blockY + 1;
		int z = pos.blockZ;
		player.setPosition(x, y, z);
		boolean flag = false;
		if (player.worldObj.blockExists(x, y, z)) {
			boolean flag1 = false;

			while (!flag1 && y > 0) {
				Block block = player.worldObj.getBlock(x, y - 1, z);
				if (block.getMaterial().blocksMovement())
					flag1 = true;
				else {
					--player.posY;
					--y;
				}
			}

			if (flag1) {
				player.setPosition(x, y, z);

				if (player.worldObj.getCollidingBoundingBoxes(player, player.boundingBox).isEmpty() && !player.worldObj.isAnyLiquid(player.boundingBox)) {
					flag = true;
				} else {
					Logger.d("debug", "CollidingBox not empty or liquid");
				}

			}
		}

		if (!flag) {
			player.setPosition(ox, oy, oz);
			player.playSound("note.bd", 1.0F, 1.0F);
			return false;
		}
		if (player instanceof EntityPlayerMP) {
			EntityPlayerMP playerMp = (EntityPlayerMP) player;
			playerMp.mountEntity(null);
			playerMp.setPositionAndUpdate(x, y, z);
		}
		player.worldObj.playSoundEffect(ox, oy, oz, "mob.endermen.portal", 1.0F, 1.0F);
		player.playSound("mob.endermen.portal", 1.0F, 1.0F);
		return true;
	}

}
