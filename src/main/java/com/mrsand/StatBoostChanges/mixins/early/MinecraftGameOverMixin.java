package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.common.packets.PacketClearDeathScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiScreen;

/**
 * After a vampire CANCEL_SAVED, block GuiGameOver reopen while suppress is active
 * (S06 health=0 can race ahead of the heal/clear packets).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftGameOverMixin {

	@Inject(method = "func_147108_a", at = @At("HEAD"), cancellable = true)
	private void bvSuppressGameOverAfterVampireSave(GuiScreen screen, CallbackInfo ci) {
		if (screen instanceof GuiGameOver && PacketClearDeathScreen.shouldSuppressGameOver()) {
			ci.cancel();
		}
	}
}
