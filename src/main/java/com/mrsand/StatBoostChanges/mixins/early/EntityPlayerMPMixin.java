package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayerMP;

@Mixin(EntityPlayerMP.class)
public abstract class EntityPlayerMPMixin {

	@Inject(method = "func_70999_a", at = @At("HEAD"), cancellable = true)
	private void wakeUpPlayerNotIfVamprisim(boolean immediately, boolean updateWorld, boolean setSpawn, CallbackInfo ci) {
		if(!Loader.isModLoaded("vampirism")) return;
		else {
			EntityPlayerMP player = (EntityPlayerMP) (Object) this;
			boolean allowedToWake = de.teamlapen.vampirism.coremod.CoreHandler.shouldWakePlayer(player, immediately, updateWorld, setSpawn);
		    if (!allowedToWake) {
		    	ci.cancel();
		        return; // Stop right here! Do not send packets, do not wake up.      
		    }
//			if(player == null) return;
//			//			GeneralUtil.sendDebugMessage(player, "EntityPlayerMP   mixin new");
//			ChunkCoordinates bedLocation = player.playerLocation;
//			//			ChunkCoordinates spawnLocation = player.playerLocation;
//			Block block = (bedLocation == null ? null : player.worldObj.getBlock(bedLocation.posX, bedLocation.posY, bedLocation.posZ));
//			TileEntity tile = player.worldObj.getTileEntity(bedLocation.posX, bedLocation.posY, bedLocation.posZ);
//
//			if(GeneralUtil.isVampire(player) && (VampirismUtil.isCoffin(block) || VampirismUtil.isCoffinTile(tile))) {
//				GeneralUtil.sendDebugMessage(player, "cancelling wakeup method for BlockCoffin! part 2!");
//
//				if (!player.worldObj.isRemote && player.worldObj.isDaytime() && !immediately) {
//					ci.cancel(); 
//				}
//			}
			//			// 1. If the player is sleeping, send a "Wake Up" animation packet to all nearby clients
			//			if (this.isPlayerSleeping())
			//			{
			//				this.getServerForPlayer().getEntityTracker().sendToAllTrackingEntity(this, new S0BPacketAnimation(this, 2));
			//			}
			//
			//			// 2. Call the parent EntityPlayer.wakeUpPlayer() method
			//			super.wakeUpPlayer(immediately, updateWorld, setSpawn);
			//
			//			// 3. Sync the player's exact physics position to the net handler
			//			if (this.playerNetServerHandler != null)
			//			{
			//				this.playerNetServerHandler.setPlayerLocation(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
			//			}


		}
	}
}
