package com.mrsand.StatBoostChanges.util;

import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.block.BlockCoffin;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.entity.player.skills.Skills;
import de.teamlapen.vampirism.tileEntity.TileEntityCoffin;
import de.teamlapen.vampirism.util.BALANCE;
import de.teamlapen.vampirism.util.Helper;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

public class VampirismUtil {

	public static boolean isVampire(EntityPlayer player) {
		return VampirePlayer.get(player).getLevel() > 0;
	}
	public static boolean isCoffin(Block block) {
		return block instanceof BlockCoffin;
	}
	public static boolean isCoffinTile(TileEntity tile) {
		return tile instanceof TileEntityCoffin;
	}

	public static boolean isBatMode(EntityPlayer player) {
		return VampirePlayer.get(player).isSkillActive(Skills.batMode);
	}
	public static boolean hasBatMode(EntityPlayer player) {
		VampirePlayer vPlayer =  VampirePlayer.get(player);
		return vPlayer.getLevel() >= BALANCE.VP_SKILLS.BAT_MIN_LEVEL;
	}
	public static void setBatMode(EntityPlayer player, boolean mode) {
		VampirePlayer vPlayer =  VampirePlayer.get(player);
		VampirismMod.proxy.setPlayerBat(player, mode);
	}
	public static boolean isInVampireBiome(Entity entity) {
		if(entity == null) return false;
		return Helper.isEntityInVampireBiome(entity);
	}

	public static void increaseVampireLevel(EntityPlayer player, int newLevel) {
		if (VampirePlayer.get(player).getLevel() != 0) return;
		VampirePlayer.get(player).levelUp();
	}

	public static void reverseBatHeights(EntityPlayer player) {
		//		VampirePlayer vPlayer =  VampirePlayer.get(player);
		//		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
		boolean bat = isBatMode(player);
		//		VampirismMod.proxy.setPlayerBat(player, false);
		//		player.se
		//		player.setPosition(player.posX, player.posY - 0.99999994F, player.posZ);
		player.capabilities.allowFlying = false;
		player.capabilities.isFlying = false;
		if(player.worldObj.isRemote) {
			float width = bat ? 0.5F : 0.6F;
			float height = bat ? 0.8F : 1.8F;
			Helper.Reflection.callMethod(Entity.class, player,
					Helper.Obfuscation.getPosNames("Entity/setSize"),
					Helper.Reflection.createArray(new Class[] { float.class, float.class }),
					new Object[] { Float.valueOf(width), Float.valueOf(height) });

			//			player.setPosition(
			//			    player.posX,
			//			    player.posY + ((bat ? 1.0F : -1.0F) * 0.99999994F),
			//			    player.posZ);

			player.eyeHeight = (bat ? 0.68F : player.getDefaultEyeHeight()) - player.yOffset;
		}
		else if (!player.worldObj.isRemote) {
			player.eyeHeight = player.getDefaultEyeHeight();
			float width = bat ? 0.5F : 0.6F;
			float height = bat ? 0.8F : 1.8F;

			Helper.Reflection.callMethod(Entity.class, player,
					Helper.Obfuscation.getPosNames("Entity/setSize"),
					Helper.Reflection.createArray(new Class[] { float.class, float.class }),
					new Object[] { Float.valueOf(width), Float.valueOf(height) });

			//			player.setPosition(
			//			    player.posX,
			//			    player.posY + ((bat ? 1.0F : -1.0F) * 0.99999994F),
			//			    player.posZ);

			player.eyeHeight = bat ? (0.68F - player.yOffset) : player.getDefaultEyeHeight();
		}
	}

}
