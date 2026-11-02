package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.VampireEyesDebug;
import com.mrsand.StatBoostChanges.util.VampireEyesVisual;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.powers.LegacyPowerVampiricEyes;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

@Mixin(value = LegacyPowerVampiricEyes.class, remap = false)
public abstract class LegacyPowerVampiricEyesMixin {

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void bv$cycleEyeMode(EntityPlayer player, CallbackInfo ci) {
		ci.cancel();
		if (player == null) return;
		LegacyPowerVampiricEyes self = (LegacyPowerVampiricEyes) (Object) this;
		int key = self.getAssignedKey(player);
		if (key <= 0) {
			VampireEyesDebug.log(player, "vampiricEyes key not assigned");
			return;
		}
		// Stock Legends: only fire when cooldown == cooldownMax (ready), not when > 0.
		int cd = LegacyHelper.getCooldown(player, key);
		int cdMax = LegacyHelper.getCooldownMax(player, key);
		if (cd != cdMax) {
			VampireEyesDebug.log(player, "vampiricEyes on cooldown cd=" + cd + " max=" + cdMax);
			return;
		}

		int before = VampireEyesVisual.getMode(player);
		int next = VampireEyesVisual.cycleMode(before);
		LegendsProperties.VAMPIRE_EYES.set(player, Integer.valueOf(next));
		LegacyHelper.setCooldown(player, key, 40);
		VampireEyesDebug.log(player, "mode " + before + " -> " + next);

		if (!player.worldObj.isRemote && StatBoostConfig.showVampireEyesModeChat) {
			GeneralUtil.sendDebugMessage(player, new ChatComponentText(VampireEyesVisual.modeChatMessage(next))
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.DARK_RED)));
		}
	}

	@Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
	private void bv$eyeModeDescription(EntityPlayer player, CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(
				"Cycles vampiric eyes mode.\n\nPowers only\nAlways shown\nDisabled");
	}
}
