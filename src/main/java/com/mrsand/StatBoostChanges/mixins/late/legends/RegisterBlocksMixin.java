package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.tihyo.legends.blocks.BlockMagma;
import com.tihyo.legends.blocks.RegisterBlocks;

import net.minecraft.block.material.Material;

@Mixin(value = RegisterBlocks.class, remap = false)
public class RegisterBlocksMixin {

    @Redirect(
        method = "initBlocks",
        at = @At(
            value = "NEW", 
            target = "com/tihyo/legends/blocks/BlockMagma"
        )
    )
    private static BlockMagma redirectMagmaConstructor(Material material) {
        // This calls the original constructor, which sets light to 1.3F...
        BlockMagma block = new BlockMagma(material);
        

        block.setLightLevel(0.0F); 
        
        return block;
    }
}
