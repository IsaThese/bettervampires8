package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;

import cpw.mods.fml.common.Loader;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.IBlockAccess;

/** Horror coffins count as beds for respawn verification / setSpawnChunk(false). */
@Mixin(Block.class)
public abstract class BlockIsBedCoffinMixin {

	@Inject(method = "isBed", at = @At("HEAD"), cancellable = true, remap = false)
	private void bvLegendsCoffinIsBed(IBlockAccess world, int x, int y, int z, EntityLivingBase sleeper,
			CallbackInfoReturnable<Boolean> cir) {
		if (!Loader.isModLoaded("legends")) return;
		Block self = (Block) (Object) this;
		if (CoffinSleepUtil.isLegendsCoffinBlock(self)) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}
}
