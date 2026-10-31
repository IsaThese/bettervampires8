package com.mrsand.StatBoostChanges.mixins.late;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.teamlapen.vampirism.block.BlockCoffin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

@Mixin(value = BlockCoffin.class, remap = false)
public abstract  class BlockCoffinVampirismMixin {

//	public BlockCoffinVampirismMixin(Material material, String name) {
//		super(material, name);
//		// TODO Auto-generated constructor stub
//	}

	@Shadow public abstract void setCoffinOccupied(World world, int x, int y, int z, @Nullable EntityPlayer player, boolean flag);
	@Inject(method = "onBlockActivated", at = @At(value = "HEAD"), cancellable = true)
	private void forceUniversalSleepTesting(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
//		cir.cancel();
//		GeneralUtil.sendDebugMessage(player, "new block activation");
//		if (world.isRemote)
//			cir.setReturnValue(true);
//		if ((world.getBlockMetadata(x, y, z) & 0xFFFFFFF8) == 0) {
//			TileEntityCoffin te = (TileEntityCoffin)world.getTileEntity(x, y, z);
//			x = te.otherX;
//			y = te.otherY;
//			z = te.otherZ;
//		} 
//		setCoffinOccupied(world, x, y, z, player, true);
//		((TileEntityCoffin)world.getTileEntity(x, y, z)).func_70296_d();
//		cir.setReturnValue(true);
//		return;
//		
//		
//	
////		if (player.isSneaking() && 
////				player.getCurrentEquippedItem() != null && player.getCurrentEquippedItem().getItem() instanceof net.minecraft.item.ItemDye)
////			cir.setReturnValue(false); 
////		if (world.provider.canRespawnHere() && world.getBiomeGenForCoords(x, z) != BiomeGenBase.hell) {
////			if ((world.getBlockMetadata(x, y, z) & 0x4) != 0) {
////				player.addChatComponentMessage((IChatComponent)new ChatComponentTranslation("text.vampirism.coffin.occupied", new Object[0]));
////				cir.setReturnValue(true);
////			} 
////			EntityPlayer.EnumStatus enumstatus = VampirePlayer.get(player).sleepInCoffinAt(x, y, z);
////			if (enumstatus == EntityPlayer.EnumStatus.OK) {
////				setCoffinOccupied(world, x, y, z, player, true);
////				((TileEntityCoffin)world.getTileEntity(x, y, z)).func_70296_d();
////				cir.setReturnValue(true);
////			} 
////			if (enumstatus == EntityPlayer.EnumStatus.NOT_POSSIBLE_NOW) {
////				player.addChatComponentMessage((IChatComponent)new ChatComponentTranslation("text.vampirism.coffin.noSleep", new Object[0]));
////			} else if (enumstatus == EntityPlayer.EnumStatus.NOT_SAFE) {
////				player.addChatComponentMessage((IChatComponent)new ChatComponentTranslation("tile.bed.notSafe", new Object[0]));
////			} 
////			cir.setReturnValue(true);
////		} 
////		player.addChatComponentMessage((IChatComponent)new ChatComponentTranslation("text.vampirism.coffin.wrong_dimension", new Object[0]));
////		cir.setReturnValue(true);
	}
}
