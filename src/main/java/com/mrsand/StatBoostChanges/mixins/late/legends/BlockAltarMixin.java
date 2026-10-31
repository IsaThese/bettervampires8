package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tihyo.horror.blocks.BlockAltar;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = BlockAltar.class, remap = false)
public abstract class BlockAltarMixin extends BlockContainer {

	public BlockAltarMixin(Material mat) { super(mat); }

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onConstructorEnd(Material mat, CallbackInfo ci) {
		this.setHarvestLevel(null, -1);
		this.setHardness(0.2F);
	}

	@Override
	public boolean canHarvestBlock(EntityPlayer player, int meta) {
		return true;
	}

	@Redirect(
			method = "<init>(Lnet/minecraft/block/material/Material;)V",
			at = @At(value = "INVOKE", target = "Lcom/tihyo/horror/blocks/BlockAltar;func_149711_c(F)Lnet/minecraft/block/Block;")
			)
	private Block redirectConstructorHardness(BlockAltar instance, float hardness) {
		return instance.setHardness(0.1F);
	}
}
