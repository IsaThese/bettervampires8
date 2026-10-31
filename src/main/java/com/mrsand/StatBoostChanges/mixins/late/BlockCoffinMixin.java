package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.blocks.BlockCoffin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

@Mixin(value = BlockCoffin.class, remap = false)
public abstract class BlockCoffinMixin {
	
	@Shadow public static void setBedOccupied(World world, int x, int y, int z, boolean p_149979_4_) {
		throw new RuntimeException("Shadow failed to bind!");
	}
	
	@Inject(method = "func_149727_a", at= @At("HEAD"), cancellable = true)
	public void onBlockActivate(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
//		if (world.provider.canRespawnHere() && world.getBiomeGenForCoords(x, z) != BiomeGenBase.hell) {
//	     EntityPlayer.EnumStatus enumstatus = player.sleepInBedAt(x, y, z);
//	      if (enumstatus == EntityPlayer.EnumStatus.NOT_SAFE) {
//	        setBedOccupied(world, x, y, z, true);
//	        EntityPlayer.EnumStatus == enumstatus
////	        player.addChatComponentMessage(new ChatComponentText("The monsters do not disturb a master of the night").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED).setBold(true).setItalic(true)));
////	        cir.setReturnValue(true);
////	        cir.cancel();
//	      }
//		
//	}
	}
	
	

}
