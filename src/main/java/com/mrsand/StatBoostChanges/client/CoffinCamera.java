package com.mrsand.StatBoostChanges.client;

import com.mrsand.StatBoostChanges.util.CoffinDebug;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/**
 * Local camera only. Legends leaves pitch at 0, so third person sits in the coffin.
 * Positive pitch looks down and places the camera above the body.
 */
@SideOnly(Side.CLIENT)
public final class CoffinCamera {

	/** Downward look. Third person then sits above the coffin, not in the ground. */
	private static final float COFFIN_PITCH = 60.0F;
	/** Vanilla {@code EntityRenderer.thirdPersonDistance} before the block ray. */
	private static final float THIRD_PERSON_DISTANCE = 4.0F;

	private static boolean active;
	private static int savedView;
	private static float savedPitch;

	private CoffinCamera() {}

	/** Enter only. A later lid click must not override F5. */
	public static void onEnter(EntityPlayer player) {
		if (active || !isLocal(player)) return;
		Minecraft mc = Minecraft.getMinecraft();
		savedView = mc.gameSettings.thirdPersonView;
		savedPitch = player.rotationPitch;
		active = true;
		mc.gameSettings.thirdPersonView = 1;
		player.rotationPitch = COFFIN_PITCH;
		player.prevRotationPitch = COFFIN_PITCH;
		CoffinDebug.logPoseNow(player, "CAMERA");
	}

	public static void onLeave(EntityPlayer player) {
		if (!active) return;
		active = false;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc != null && mc.gameSettings != null) {
			mc.gameSettings.thirdPersonView = savedView;
		}
		if (player != null) {
			player.rotationPitch = savedPitch;
			player.prevRotationPitch = savedPitch;
		}
	}

	/** Eye anchor and the unclipped third-person point. Empty when this is not the local view. */
	public static String describe(EntityPlayer player) {
		if (!isLocal(player)) return "";
		Minecraft mc = Minecraft.getMinecraft();
		int view = mc.gameSettings.thirdPersonView;
		float yaw = player.rotationYaw;
		float pitch = player.rotationPitch;
		if (view == 2) {
			pitch += 180.0F;
		}
		double eyeY = player.posY - (player.yOffset - 1.62F);
		double camX = player.posX;
		double camY = eyeY;
		double camZ = player.posZ;
		if (view > 0) {
			float yawRad = yaw / 180.0F * (float) Math.PI;
			float pitchRad = pitch / 180.0F * (float) Math.PI;
			double d4 = (-MathHelper.sin(yawRad) * MathHelper.cos(pitchRad)) * THIRD_PERSON_DISTANCE;
			double d5 = (MathHelper.cos(yawRad) * MathHelper.cos(pitchRad)) * THIRD_PERSON_DISTANCE;
			double d6 = (-MathHelper.sin(pitchRad)) * THIRD_PERSON_DISTANCE;
			camX = player.posX - d4;
			camY = eyeY - d6;
			camZ = player.posZ - d5;
		}
		return "view=" + view
				+ " pitch=" + String.format("%.1f", player.rotationPitch)
				+ " yaw=" + String.format("%.1f", yaw)
				+ " eyeY=" + String.format("%.3f", eyeY)
				+ " cam=" + String.format("%.2f,%.2f,%.2f", camX, camY, camZ)
				+ " camY-posY=" + String.format("%.3f", camY - player.posY);
	}

	private static boolean isLocal(EntityPlayer player) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote) return false;
		Minecraft mc = Minecraft.getMinecraft();
		return mc != null && mc.thePlayer == player && mc.gameSettings != null;
	}
}
