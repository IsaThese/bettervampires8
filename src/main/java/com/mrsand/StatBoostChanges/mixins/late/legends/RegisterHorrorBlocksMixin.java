package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.tihyo.horror.blocks.BlockAltar;
import com.tihyo.horror.blocks.RegisterBlocksHorror;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

@Mixin(value = RegisterBlocksHorror.class, remap = false)
public abstract class RegisterHorrorBlocksMixin {

	@Redirect(
			method = "initBlocks",
			at = @At(value = "NEW", target = "com/tihyo/horror/blocks/BlockAltar")
			)
	private static BlockAltar hijackAltarCreation(Material mat) {
		return new BlockAltar(Material.wood);
	}

	@Redirect(
			method = "initBlocks",
			at = @At(value = "INVOKE", target = "Lcom/tihyo/horror/blocks/BlockAltar;func_149711_c(F)Lnet/minecraft/block/Block;", ordinal = 0)
			)
	private static Block fixHardness(BlockAltar instance, float hardness) {
		return instance.setHardness(0.2F);
	}

	@Redirect(
			method = "initBlocks",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;func_149752_b(F)Lnet/minecraft/block/Block;", ordinal = 0)
			)
	private static Block fixResistance(Block instance, float resistance) {
		return instance.setResistance(5.0F);
	}
}
