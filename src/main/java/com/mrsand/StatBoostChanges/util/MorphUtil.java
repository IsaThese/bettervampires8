package com.mrsand.StatBoostChanges.util;

import com.mrsand.StatBoostChanges.common.management.StringPlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;

import morph.api.Api;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;

public class MorphUtil {
	public static EntityLivingBase getMorph(String playerName, boolean isClient) {
		return Api.getMorphEntity(playerName, isClient);
	}
	public static void forceDemorph(EntityPlayerMP player) {
		Api.forceDemorph(player);
	}
	public static void forceMorph(EntityPlayerMP player, EntityLivingBase entity) {
		Api.forceMorph(player, entity);
	}
	
	public static boolean isPlayerMorphed(EntityPlayerMP player) {
	    // Returns true if they are currently turned into any entity, false if they are normal human
	    return Api.hasMorph(player.getCommandSenderName(), player.worldObj.isRemote);
	}
	//Returns float from 0-1
	public static float getMorphTime(EntityPlayerMP player) {
		return Api.morphProgress(player.getCommandSenderName(), player.worldObj.isRemote);
	}
	
	public static void saveMorph(EntityPlayerMP player) {
	    if (player.worldObj.isRemote) return;

	    // 1. Try to get the current active morph
	    EntityLivingBase entity = Api.getMorphEntity(player.getCommandSenderName(), false);
	    
	    // 2. Fallback: If they already started demorphing, grab the previous one
	    if (entity == null) {
	        entity = Api.getPrevMorphEntity(player.getCommandSenderName(), false);
	    }

	    VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
	    if (vPlayer != null && entity != null) {
	        String className = entity.getClass().getName(); 
	        vPlayer.setString(StringPlayerKeys.LAST_ENTITYLIVING, className);
	        vPlayer.syncClient();
	    }
	}
	public static void clearMorph(EntityPlayerMP player) {
		if (player.worldObj.isRemote) return; // Server-side only
		EntityLivingBase entity = Api.getMorphEntity(player.getCommandSenderName(), false);
		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);

		if (vPlayer != null) {
			vPlayer.setString(StringPlayerKeys.LAST_ENTITYLIVING, "");
			vPlayer.syncClient();
		}
	}
	
	//Doesn't work lol
	public static void reMorphIntoLast(EntityPlayerMP player) {
	    if (player.worldObj.isRemote) return; 

	    VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
	    if (vPlayer == null) return;

	    String className = vPlayer.getString(StringPlayerKeys.LAST_ENTITYLIVING);

	    if (className != null && !className.isEmpty()) {
	        try {
	            // 1. Locate the Java class file from the saved string path
	            Class<?> clazz = Class.forName(className);
	            
	            // 2. Instantiate the entity by calling its (World) constructor dynamically
	            EntityLivingBase reconstructedEntity = (EntityLivingBase) clazz.getConstructor(net.minecraft.world.World.class).newInstance(player.worldObj);

	            if (reconstructedEntity != null) {
	                // 3. Hand the fresh entity token off to the Morph Mod API
	                Api.forceMorph(player, reconstructedEntity);
	            }
	        } catch (Exception e) {
	            // Catch reflection errors (e.g., if a mod was removed and the class path no longer exists)
	            System.err.println("[BetterVampires] Failed to reflect and reconstruct entity class: " + className);
	            e.printStackTrace();
	        }
	    }
	}


}
