package com.mrsand.StatBoostChanges.util;

import java.util.List;

import com.emoniph.witchery.entity.EntityVillageGuard;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.EnumDifficulty;

public class SuspicionUtil {

	public static void setSuspicion(EntityPlayer player, int amount) {
		if(!player.worldObj.isRemote) {
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
			vPlayer.setInteger(PlayerKeys.SUSPICION, amount);
			vPlayer.syncClient();
		}
	
	}

	public static int getSuspicion(EntityPlayer player) {
		return VExtendedPlayer.get(player).getInteger(PlayerKeys.SUSPICION);
	}

	public static void decreaseSuspicion(EntityPlayer player, int decrease) {
		if(!player.worldObj.isRemote) {
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
			vPlayer.setInteger(PlayerKeys.SUSPICION, Math.max(0, getSuspicion(player) - decrease));
			vPlayer.syncClient();
		}
	}

	public static void increaseSuspicion(EntityPlayer player, int increase) {
		if (!player.capabilities.isCreativeMode && !player.worldObj.isRemote && player.worldObj.difficultySetting != EnumDifficulty.PEACEFUL) {
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
			vPlayer.setInteger(PlayerKeys.SUSPICION, Math.min(6090, getSuspicion(player) + increase));
			vPlayer.syncClient();
		}
		else if (player.capabilities.isCreativeMode)
			setSuspicion(player, 0);
	}

	public static boolean checkForSuspicionWitnesses(EntityPlayer player, int increase) {
		return SuspicionUtil.checkForSuspicionWitnesses(player, increase, 32);
	}

	public static boolean checkForSuspicionWitnesses(EntityPlayer player, int increase, double range) {
		return SuspicionUtil.checkForSuspicionWitnesses(player, increase, range, "", true);
	}

	public static boolean checkForSuspicionWitnesses(EntityPlayer player, int increase, String message, boolean send) {
		return SuspicionUtil.checkForSuspicionWitnesses(player, increase, 32, message, send);
	}
	public static boolean checkForSuspicionWitnesses(EntityPlayer player, int increase, double range, String message, boolean send, boolean forced) {
		if (LegendsUtil.isHidingMorph(player) && !forced)
			return false;
        return checkForSuspicionWitnesses(player, increase, range, message, send) ;
    }

	//Witchery Code
	public static boolean checkForSuspicionWitnesses(EntityPlayer player, int increase, double range, String message, boolean send) {
		if (GeneralUtil.getClosestVillage(player, 128) != null && !player.capabilities.isCreativeMode
				&& !player.worldObj.isRemote) {
			// boolean witnessed = false;
			NBTTagCompound playerNBT = player.getEntityData();
			String victimUUID = playerNBT.getString("CurrentBiteTargetUUID");

			AxisAlignedBB bounds = player.boundingBox.expand(range, 16.0D, range);
			List<EntityVillager> villagers = player.worldObj.getEntitiesWithinAABB(EntityVillager.class, bounds);

			for (EntityVillager villager : villagers) {
				if (villager.canEntityBeSeen(player) && !villager.isTrading() && villager.hasHome()
						&& !WitcheryUtil.isKnockedOut(villager) && !villager.isDead) {
					// witnessed = true;
					//					EntityVillager villager = (EntityVillager) event.entityLiving;
					NBTTagCompound nbt = villager.getEntityData();

					if (nbt.hasKey("ReportingTimer") || (!victimUUID.isEmpty() && villager.getUniqueID().toString().equals(victimUUID))
							|| !villager.hasHome() || villager.villageObj == null) {
						continue;
					}


					increaseSuspicion(player, increase);
					boolean notify = false;
					if (send)
						notify = true;
					else if (player.worldObj.rand.nextInt(5) == 0 || !message.isEmpty())
						notify = true;
					if (notify) {

						// Make the villager stop what they're doing and look at the player
						villager.getLookHelper().setLookPositionWithEntity(player, 30.0F, 30.0F);

						// Play the "No" sound (the annoyed 'Hmph')
						// We randomize the pitch (0.8 to 1.2) so they don't all sound identical
						float pitch = 0.8F + player.worldObj.rand.nextFloat() * 0.4F;
						player.worldObj.playSoundAtEntity(villager, "mob.villager.no", 3.0F, pitch);

						// Visual "Head Shake" (This triggers the 'No' particle/animation in 1.7.10)
						player.worldObj.setEntityState(villager, (byte) 13);
					}
					if (send) {
						if (!message.isEmpty())
							GeneralUtil.sendDebugMessage(player, new ChatComponentText(message).setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
						else
							GeneralUtil.sendDebugMessage(player, new ChatComponentText("You have been witnessed").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));

					}
					return true;
				}
				// break;
			}
		}
		if (Loader.isModLoaded("witchery"))
			return SuspicionUtil.findWatchingGuards(player, increase, range, message, send);
		return false;
	}

		public static boolean findWatchingGuards(EntityPlayer player, int increase, double range, String message, boolean send) {
			if(Loader.isModLoaded("witchery")) {
				if (GeneralUtil.getClosestVillage(player, 128) != null && !player.capabilities.isCreativeMode
						&& !player.worldObj.isRemote) {
					// boolean witnessed = false;
					NBTTagCompound playerNBT = player.getEntityData();
					String victimUUID = playerNBT.getString("CurrentBiteTargetUUID");
		
					AxisAlignedBB bounds = player.boundingBox.expand(range, 16.0D, range);
					List<EntityVillageGuard> villagers = player.worldObj.getEntitiesWithinAABB(EntityVillageGuard.class, bounds);
		
					for (EntityVillageGuard villager : villagers) {
						if (!victimUUID.isEmpty() && villager.getUniqueID().toString().equals(victimUUID)) {
							continue; 
						}
						if (villager.canEntityBeSeen(player) && villager.hasHome()
								&& !WitcheryUtil.isKnockedOut(villager) && !villager.isDead) {
							increaseSuspicion(player, increase);
							boolean notify = false;
							if(send)
								notify = true;
							else if (player.worldObj.rand.nextInt(5) == 0 || !message.isEmpty())
								notify = true;				
							if (notify) {
		
								// Make the villager stop what they're doing and look at the player
								villager.getLookHelper().setLookPositionWithEntity(player, 30.0F, 30.0F);
		
								// Play the "No" sound (the annoyed 'Hmph') 
								// We randomize the pitch (0.8 to 1.2) so they don't all sound identical
								float pitch = 0.8F + player.worldObj.rand.nextFloat() * 0.4F;
								player.worldObj.playSoundAtEntity(villager, "mob.villager.no", 3.0F, pitch);
		
								// Visual "Head Shake" (This triggers the 'No' particle/animation in 1.7.10)
								player.worldObj.setEntityState(villager, (byte)13); 
							}
							if(send) {
								if(!message.isEmpty())
									GeneralUtil.sendDebugMessage(player, new ChatComponentText(message).setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
								else
									GeneralUtil.sendDebugMessage(player, new ChatComponentText("You have been witnessed").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
		
							}
							return true;
						}
						// break;
					}
				}
		
			}
			return false;
		}

}
