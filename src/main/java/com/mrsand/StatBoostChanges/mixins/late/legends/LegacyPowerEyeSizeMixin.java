package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.VampireEyesDebug;
import com.mrsand.StatBoostChanges.util.VampireEyesVisual;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.powers.LegacyPowerEyeSize;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

@Mixin(value = LegacyPowerEyeSize.class, remap = false)
public abstract class LegacyPowerEyeSizeMixin {

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void bv$cycleEyeSize(EntityPlayer player, CallbackInfo ci) {
		ci.cancel();
		if (player == null) return;
		LegacyPowerEyeSize self = (LegacyPowerEyeSize) (Object) this;
		int key = self.getAssignedKey(player);
		if (key <= 0) {
			VampireEyesDebug.log(player, "eyeSize key not assigned");
			return;
		}
		int cd = LegacyHelper.getCooldown(player, key);
		int cdMax = LegacyHelper.getCooldownMax(player, key);
		if (cd != cdMax) {
			VampireEyesDebug.log(player, "eyeSize on cooldown cd=" + cd + " max=" + cdMax);
			return;
		}

		int before = PlayerHelper.getEyeType(player);
		int next = VampireEyesVisual.cycleEyeType(before);
		LegendsProperties.EYE_TYPE.set(player, Integer.valueOf(next));
		LegacyHelper.setCooldown(player, key, 40);
		VampireEyesDebug.log(player, "eyeType " + before + " -> " + next);

		if (!player.worldObj.isRemote) {
			String label;
			switch (next) {
			case VampireEyesVisual.EYE_TYPE_2X2:
				label = "2x2";
				break;
			case VampireEyesVisual.EYE_TYPE_NONE:
				label = "None";
				break;
			case VampireEyesVisual.EYE_TYPE_BV_1X2:
				label = "BV 1x2";
				break;
			case VampireEyesVisual.EYE_TYPE_1X2:
			default:
				label = "1x2";
				break;
			}
			GeneralUtil.sendDebugMessage(player, new ChatComponentText("Eye size: " + label)
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.DARK_RED)));
		}
	}

	@Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
	private void bv$eyeSizeDescription(EntityPlayer player, CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(VampireEyesVisual.eyeSizeDescription());
	}
}
