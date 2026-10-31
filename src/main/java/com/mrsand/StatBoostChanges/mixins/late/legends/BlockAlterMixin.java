package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tihyo.horror.blocks.BlockAlter;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = BlockAlter.class, remap = false)
public abstract class BlockAlterMixin extends BlockContainer {

    public BlockAlterMixin(Material mat) { super(mat); }

    /**
     * Redirects the call to setHarvestLevel inside the BlockAlter constructor.
     * Original code: setHarvestLevel("pickaxe", 2);
     */
//    @Redirect(
//        method = "<init>(Lnet/minecraft/block/material/Material;)V",
//        at = @At(value = "INVOKE", target = "Lcom/tihyo/horror/blocks/BlockAlter;setHarvestLevel(Ljava/lang/String;I)V")
//    )
//    public void redirectHarvestLevel(BlockAlter instance, String toolClass, int level) {
//        // We call it with null/-1 instead of "pickaxe"/2
//        instance.setHarvestLevel(null, -1);
//    }
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructorEnd(Material mat, CallbackInfo ci) {
    	BlockAlter self = (BlockAlter)(Object)this;
        // Clear the pickaxe requirement so it's hand-breakable
        this.setHarvestLevel(null, -1);
        // Force the low hardness in case the constructor set it to 3.5F
        this.setHardness(0.2F);
    }
    @Override
    public boolean canHarvestBlock(EntityPlayer player, int meta) {
        return true;
    }
    /**
     * Redirects the call to setHardness (func_149711_c) inside the constructor.
     * Original code: func_149711_c(3.5F);
     */
    @Redirect(
        method = "<init>(Lnet/minecraft/block/material/Material;)V",
        at = @At(value = "INVOKE", target = "Lcom/tihyo/horror/blocks/BlockAlter;func_149711_c(F)Lnet/minecraft/block/Block;")
    )
    private Block redirectConstructorHardness(BlockAlter instance, float hardness) {
        // Force the internal hardness to be low immediately
        return instance.setHardness(0.1F);
    }
}