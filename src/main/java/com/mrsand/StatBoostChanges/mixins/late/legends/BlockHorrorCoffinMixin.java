package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;
import com.tihyo.horror.blocks.BlockCoffin;
import com.tihyo.horror.blocks.BlockCoffinBlack;
import com.tihyo.horror.blocks.BlockCoffinBlue;
import com.tihyo.horror.blocks.BlockCoffinGray;
import com.tihyo.horror.blocks.BlockCoffinPink;
import com.tihyo.horror.blocks.BlockCoffinRed;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * After Legends coffin activate: bind nest on enter; sneak-open while inside → exit.
 */
@Mixin(value = {
		BlockCoffin.class,
		BlockCoffinBlack.class,
		BlockCoffinGray.class,
		BlockCoffinRed.class,
		BlockCoffinBlue.class,
		BlockCoffinPink.class
}, remap = false)
public abstract class BlockHorrorCoffinMixin {

	@Inject(method = "func_149727_a", at = @At("RETURN"))
	private void bvAfterCoffinActivate(World world, int x, int y, int z, EntityPlayer player, int side,
			float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		if (player != null) {
			CoffinSleepUtil.afterCoffinActivated(player, world, x, y, z);
		}
	}
}
