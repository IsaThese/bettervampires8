package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.item.ItemCaneSword;
import com.emoniph.witchery.util.ParticleEffect;
import com.emoniph.witchery.util.SoundEffect;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

@Mixin(value = ItemCaneSword.class, remap = false)
public abstract class ItemCaneSwordMixin extends ItemSword{

	public ItemCaneSwordMixin(ToolMaterial p_i45356_1_) {
		super(p_i45356_1_);
	}

	@Shadow protected abstract boolean isDrawn(NBTTagCompound nbtItem);
	@Shadow protected abstract void setDrawn(EntityPlayer player, ItemStack stack, NBTTagCompound nbtItem, boolean deployed);

	@Inject(method = "func_77659_a", at = @At("HEAD"), cancellable = true)
	public void onItemRightClick(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable <ItemStack>cir) {
		cir.cancel();
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		NBTTagCompound nbt = stack.getTagCompound();
		boolean isBladeOut = isDrawn(nbt);

		if (player.isSneaking()) {
			// TOGGLE BLADE: Sneak + Right Click
			if (!world.isRemote) {
				setDrawn(player, stack, nbt, !isBladeOut);
				if (!isBladeOut) { // Logic seems flipped in original: sounds for draw/sheathe
					SoundEffect.WITCHERY_RANDOM_SWORD_DRAW.playAtPlayer(world, player, 1.0F, 1.0F);
				} else {
					SoundEffect.WITCHERY_RANDOM_SWORD_SHEATHE.playAtPlayer(world, player, 1.0F, 1.0F);
				}
			}
		} else if (isBladeOut) {
			// SWORD BEHAVIOR: Standard sword block/use
			super.onItemRightClick(stack, world, player);
		} else {
			// CANE BEHAVIOR: Use Blood Reserve
			ExtendedPlayer playerEx = ExtendedPlayer.get(player);
			if (playerEx.isVampire() && playerEx.isBloodReserveReady() && playerEx.getBloodPower() < playerEx.getMaxBloodPower()) {
				// Play particles and "drink" from the cane handle
				ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, world, player.posX, player.posY + player.height * 0.85D, player.posZ, 0.5D, 0.5D, 16);
				
				playerEx.useBloodReserve();
			} else {
				// Failed to drink (not a vampire or reserve empty)
				SoundEffect.NOTE_SNARE.playOnlyTo(player);
			}
		}
		cir.setReturnValue(stack);
	}



}
