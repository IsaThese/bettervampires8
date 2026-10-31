package com.mrsand.StatBoostChanges.mixins.late;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.common.ExtendedPlayer.VampirePower;
import com.emoniph.witchery.item.ItemGeneral;
import com.emoniph.witchery.item.ItemGeneral.SubItem;
import com.emoniph.witchery.util.BlockUtil;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

@Mixin(value = ItemGeneral.class, remap = false)
public abstract class ItemGeneralMixin {

	@Shadow
	public abstract boolean useNecroStone(World world, EntityPlayer player, ItemStack itemstack);

	@Shadow
	private SubItem itemNecroStone;
	@Shadow
	public final ArrayList<SubItem> subItems = new ArrayList<SubItem>();

	@Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
	private void necroRightClick(ItemStack itemStack, World world, EntityPlayer player,
			CallbackInfoReturnable<ItemStack> cir) {

		SubItem subItem = subItems.get(itemStack.getItemDamage());

		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		if (this.itemNecroStone.isMatch(itemStack)
				|| (player.getCurrentEquippedItem() == null && playerEx.isVampire())) {

			useNecroStone(world, player, itemStack);
			player.addChatMessage(new ChatComponentText("Your hand is empty and are a vampire!"));

		}
	}


	@Shadow public static void teleportToLocation(World world, double posX, double posY, double posZ, int dimension, Entity entity, boolean presetPosition) {}

	@Inject(method = "teleportToLocationSafely", at = @At("HEAD"), cancellable = true)
	private static void teleportToLocationSafely(World world, double posX, double posY, double posZ, int dimension, Entity entity, boolean presetPosition, CallbackInfoReturnable<Boolean> cir) {
		
		if (world.isRemote) return;
		
	    // Obtain the WorldServer for the target dimension to perform remote safety checks
	    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(dimension);
	    if (worldServer == null) {
	        cir.setReturnValue(false);
	        return;
	    }

	    int x = MathHelper.floor_double(posX);
	    int y = MathHelper.floor_double(posY);
	    int z = MathHelper.floor_double(posZ);

	    // Safety Search: Checks -16 to +16 blocks vertically in the target dimension
	    for (int i = 0; i < 16; i++) {
	        int[] heights = (i == 0) ? new int[]{y} : new int[]{y + i, y - i};

	        for (int dy : heights) {
	            if (dy < 1 || dy > 250) continue;

	            boolean isSafeFromSun = true;
	            if (entity instanceof EntityPlayer && GeneralUtil.isVampire((EntityPlayer)entity)) {
	                // Check sun safety in the target dimension's WorldServer context
	                if (worldServer.isDaytime() && worldServer.canBlockSeeTheSky(x, dy + 1, z)) {
	                    isSafeFromSun = false;
	                }
	            }

	            // Structural Integrity Check: Solid floor + 2 blocks of air headspace in target dimension
	            if (!BlockUtil.isReplaceableBlock((World)worldServer, x, dy, z)      
	                    && BlockUtil.isReplaceableBlock((World)worldServer, x, dy + 1, z)   
	                    && BlockUtil.isReplaceableBlock((World)worldServer, x, dy + 2, z)   
	                    && isSafeFromSun)                                                   
	            {
	                if (entity instanceof EntityPlayer) {
	                    EntityPlayer player = (EntityPlayer) entity;
	                    NBTTagCompound nbt = player.getEntityData();
	                    if(nbt.getInteger("isInCombat") == 1) {
	        				nbt.getInteger("isInCombatTimer");
	        				GeneralUtil.sendDebugMessage(player, new ChatComponentText("Can't teleport in combat, timer: " + Math.round(nbt.getInteger("isInCombatTimer")/20) + "s").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
	        				cir.setReturnValue(false);
	        				return;
	        			}

	                    
	                    // Ultimate Vampire logic: Bring enslaved villagers across dimensions
	                    if(GeneralUtil.isVampire(player) && ExtendedPlayer.get(player).getSelectedVampirePower() == VampirePower.ULTIMATE) {
	                        List<Entity> nearbyEntities = GeneralUtil.getEntityListFromEntity(player, 24);
	                        for (Entity nearEntity : nearbyEntities) {
	                        	if (nearEntity instanceof EntityLiving) {
	                        	    EntityLiving nearLiving = (EntityLiving) nearEntity;
	                        	    boolean isEnslaved = WitcheryUtil.isEnslavedBy(player, nearLiving);

	                        	    if (isEnslaved) {
	                        	        if (nearLiving instanceof EntityVillager || nearLiving instanceof EntityAnimal || nearLiving instanceof EntityMob) {
	                        	        	if (nearLiving.ridingEntity != null) {
	                        	                nearLiving.mountEntity(null);
	                        	            }
	                        	            teleportToLocation(world, x + 0.7, dy + 1.0, z + 0.5, dimension, nearLiving, presetPosition);
	                        	            if (nearLiving.worldObj instanceof WorldServer) {
	                        	                WorldServer ws = (WorldServer) nearLiving.worldObj;
	                        	                EntityTracker tracker = ws.getEntityTracker();
	                        	                
	                        	                tracker.removeEntityFromAllTrackingPlayers(nearLiving); 
	                        	                
	                        	                tracker.addEntityToTracker(nearLiving); 
	                        	            }
	                        	            
	                        	            String typeName = (nearLiving instanceof EntityVillager) ? " villager" : (nearLiving instanceof EntityAnimal) ? "n animal": " mob";    	     
	                        	            player.addChatComponentMessage(new ChatComponentText("A" + typeName + " teleports with you")
	                        	                .setChatStyle(new ChatStyle().setBold(true).setItalic(true).setColor(EnumChatFormatting.RED)));
	                        	            break; 
	                        	        }
	                        	    }
	                        	}
	                        }
	                    }
	                    SuspicionUtil.checkForSuspicionWitnesses(player, 1000);
	                }

	                // Execute final movement. teleportToLocation triggers travelToDimension if dim differs.
	                teleportToLocation(world, x + 0.5, dy + 1.0, z + 0.5, dimension, entity, presetPosition);
	                
	                if(entity instanceof EntityPlayer)
	                    SuspicionUtil.checkForSuspicionWitnesses((EntityPlayer)entity, 1000);
	                
	                cir.setReturnValue(true);
	                return;
	            }
	        }
	    }
	    cir.setReturnValue(false);
	}
}