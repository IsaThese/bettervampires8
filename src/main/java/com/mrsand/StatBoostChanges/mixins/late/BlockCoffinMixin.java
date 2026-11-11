package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.blocks.BlockCoffin;
import com.mrsand.StatBoostChanges.util.GeneralUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

/** Nudge vampires toward Legends coffins; block Witchery coffin sleep UX. */
@Mixin(value = BlockCoffin.class, remap = false)
public abstract class BlockCoffinMixin {

	@Inject(method = "func_149727_a", at = @At("HEAD"), cancellable = true)
	private void bvDenyWitcheryCoffinForVampires(World world, int x, int y, int z, EntityPlayer player, int side,
			float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		if (world.isRemote || player == null) return;
		if (!GeneralUtil.isVampire(player)) return;
		player.addChatComponentMessage(new ChatComponentText("Use a Legends coffin to rest.")
				.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED).setItalic(true)));
		cir.setReturnValue(Boolean.TRUE);
	}
}
