package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.tihyo.horror.blocks.BlockAlter;
import com.tihyo.horror.blocks.RegisterBlocksHorror;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

@Mixin(value = RegisterBlocksHorror.class, remap = false)
public abstract class RegisterHorrorBlocksMixin {

    /**
     * Redirects 'new BlockAlter(Material.iron)'
     * remap = false ensures Mixin doesn't try to change the names we provided.
     */
    @Redirect(
        method = "initBlocks",
        at = @At(value = "NEW", target = "BlockAlter")
    )
    private static BlockAlter hijackAltarCreation(Material mat) {
        return new BlockAlter(Material.wood);
    }

    /**
     * Target: func_149711_c (setHardness)
     * Body: setHardness (clean)
     */
    @Redirect(
        method = "initBlocks",
        at = @At(value = "INVOKE", target = "func_149711_c", ordinal = 0)
    )
    private static Block fixHardness(BlockAlter instance, float hardness) {
        return instance.setHardness(0.2F);
    }

    /**
     * Target: func_149752_b (setResistance)
     * Body: setResistance (clean)
     */
    @Redirect(
        method = "initBlocks",
        at = @At(value = "INVOKE", target = "func_149752_b", ordinal = 0)
    )
    private static Block fixResistance(Block instance, float resistance) {
        return instance.setResistance(5.0F);
    }
}