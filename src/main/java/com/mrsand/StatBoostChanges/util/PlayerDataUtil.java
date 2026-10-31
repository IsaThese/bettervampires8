package com.mrsand.StatBoostChanges.util;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Multimap;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class PlayerDataUtil {

	public static void decreaseXP(EntityPlayer player, int pointsToRemove) {
	
		int newTotal = player.experienceTotal - pointsToRemove;
		if (newTotal < 0) newTotal = 0;
	
		// 2. Set the fields DIRECTLY (This avoids the sound logic)
		player.experienceTotal = newTotal;
	
		// 3. Recalculate level and bar percentage from the total
		// Level 0-15: 17 xp per level | 16-30: linear increase | 31+: steeper increase
		int level = 0;
		int xpRemaining = newTotal;
	
		while (true) {
			int cap = (level >= 30 ? 62 + (level - 30) * 7 : (level >= 15 ? 17 + (level - 15) * 3 : 17));
			if (xpRemaining < cap) break;
			xpRemaining -= cap;
			level++;
		}
	
		player.experienceLevel = level;
		int finalCap = (level >= 30 ? 62 + (level - 30) * 7 : (level >= 15 ? 17 + (level - 15) * 3 : 17));
		player.experience = (float)xpRemaining / (float)finalCap;
	
		// 4. Manually Sync to Client (Crucial so the HUD updates)
		if (player instanceof EntityPlayerMP) {
			((EntityPlayerMP)player).playerNetServerHandler.sendPacket(
					new S1FPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel)
					);
		}
	}

	/**
	 * Deobfuscated version of the XP reduction logic.
	 */
	public static void decreaseXPMine(EntityPlayer player, int pointsToRemove) {
		// 1. Adjust the internal total cumulative XP points
		player.experienceTotal -= pointsToRemove;
	
		// 2. Adjust the current bar progress (the float between 0.0 and 1.0)
		// We cast to float to ensure we get a decimal percentage
		player.experience -= (float)pointsToRemove / (float)player.xpBarCap();
	
		// 3. Handle Level Down Logic
		// If the bar progress is negative, it means we've dropped at least one level
		while (player.experience < 0.0F) {
			// Convert the negative percentage back into "debt" points
			float debtPoints = player.experience * (float)player.xpBarCap();
	
			// If we are at the very bottom (Level 0) and still have debt, stop at 0
			if (player.experienceLevel <= 0) {
				player.experienceLevel = 0;
				player.experience = 0.0F;
				player.experienceTotal = 0;
				break; 
			}
	
			// Move down one level
			player.addExperienceLevel(-1);
	
			// Recalculate the bar progress based on the new (lower) level's cap
			// Example: If we were -2 points into the debt and new level cap is 17, 
			// the bar becomes 1.0 + (-2/17) = 0.88 (88% full)
			player.experience = 1.0F + (debtPoints / (float)player.xpBarCap());
		}
	
		// Safety check for underflow
		if (player.experienceTotal < 0) {
			player.experienceTotal = 0;
		}
		if (player instanceof EntityPlayerMP) {
			EntityPlayerMP playerMP = (EntityPlayerMP) player;
	
			// This packet sends the current bar progress, total XP, and level to the client
			playerMP.playerNetServerHandler.sendPacket(
					new S1FPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel)
					);
		}
	}

	public static EntityPlayer loadUUIDFromNBT(NBTTagCompound nbt, String key, World world) {
		String uuidString = nbt.getString(key);
	
		if (uuidString.isEmpty())
			return null;
	
		try {
			UUID id = UUID.fromString(uuidString);
	
			// In 1.7.10, there is no direct "getPlayerByUUID" in the World class.
			// You have to iterate the list manually:
			List<EntityPlayer> players = world.playerEntities;
			for (EntityPlayer p : players) {
				if (p.getUniqueID().equals(id)) {
					return p;
				}
			}
		} catch (IllegalArgumentException e) {
			// Handle bad UUID strings
		}
	
		return null; // Player not found or offline
	}

	public static double getAttackDamage(ItemStack stack) {
		if (stack == null || stack.getItem() == null)
			return 0.0D;
	
		Multimap<String, AttributeModifier> map = stack.getAttributeModifiers();
		if (map != null) {
			for (AttributeModifier mod : map.get(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName())) {
				return mod.getAmount();
			}
		}
		return 0.0D;
	}

	public static boolean isWeapon(ItemStack stack) {
		if (getAttackDamage(stack) > 0) {
			return true;
		}
	
		return false;
	
	}

	public static void setControlling(EntityPlayer player, boolean yes) {
		int control = 0;
		if (yes)
			control = 1;
		NBTTagCompound nbtPlayer = player.getEntityData();
		nbtPlayer.setInteger("isControlling", control);
		String message = "You lessen control";
		if (control == 1) {
			message = "Their minds will follow";
		}
		GeneralUtil.sendDebugMessage(player, new ChatComponentText(message).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
	}

	public static boolean isControlling(EntityPlayer player) {
		NBTTagCompound nbtPlayer = player.getEntityData();
		if (nbtPlayer.getInteger("isControlling") == 1) {
			return true;
		}
		return false;
	}
}