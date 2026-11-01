package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.PlayerRenderHelper;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Legends 8 rewrote type-0 aiming math (~π/4–π/2 off vs Legends 5).
 * Gate on synced AIMING_RIGHT / TYPE (what observers and ReplayMod see), not
 * local-only ability duration — otherwise remotes keep stock L8 type-0 math.
 */
@Mixin(value = PlayerRenderHelper.class, remap = false)
public abstract class PlayerRenderHelperHypnosisAimMixin {

	@Inject(method = "setRotationAngles", at = @At("RETURN"))
	private static void bv$l5HypnosisAim(ModelBiped model, float limbSwing, float limbSwingAmount,
			float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		if (!(entity instanceof EntityPlayer) || model == null || model.bipedRightArm == null || model.bipedHead == null) {
			return;
		}
		EntityPlayer player = (EntityPlayer) entity;
		if (!PlayerHelper.isAimingRightArm(player)) return;
		if (PlayerHelper.getRightArmAimingType(player) != 0) return;
		if (player.isSwingInProgress) return;

		ModelRenderer arm = model.bipedRightArm;
		ModelRenderer head = model.bipedHead;
		arm.rotateAngleX = 0.0F;
		arm.rotateAngleY = 0.0F;
		arm.rotateAngleZ = 0.0F;

		float pitch = player.rotationPitch;
		if (pitch > -50.0F && pitch < 30.0F) {
			arm.rotateAngleX = head.rotateAngleX - 1.5707964F;
		} else if (pitch > 30.0F) {
			arm.rotateAngleX = head.rotateAngleX - 1.6707964F;
		} else {
			arm.rotateAngleX = head.rotateAngleX - 1.2707964F;
		}

		// L5 absolute-yaw branches almost always fall through to head.Y in real play.
		arm.rotateAngleY = head.rotateAngleY;
	}
}
