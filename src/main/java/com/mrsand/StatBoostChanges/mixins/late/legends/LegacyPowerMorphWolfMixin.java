package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.MorphUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.powers.LegacyPowerMorphWolf;
import com.tihyo.legends.morph.MorphHelper;
import com.tihyo.legends.morph.MorphRegistry;

import cpw.mods.fml.common.Loader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

@Mixin(value = LegacyPowerMorphWolf.class, remap = false)
public abstract class LegacyPowerMorphWolfMixin{

	@Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
	private void renderRightHand(Minecraft mc, EntityPlayer player, CallbackInfo ci) {
		GL11.glPushMatrix();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(new ResourceLocation("bettervampires", "textures/wolf_hand.png"));
		RenderPlayer render = (RenderPlayer) RenderManager.instance.getEntityRenderObject(player);
		render.modelBipedMain.onGround = 0.0F;
		render.modelBipedMain.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
		render.modelBipedMain.bipedRightArm.render(0.0625F);

		GL11.glPopMatrix();
		ci.cancel();
	}

	@Shadow
	public abstract int getAssignedKey(EntityPlayer player);

	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
	private void executeMorph(EntityPlayer player, CallbackInfo cir) {	
		if (LegacyHelper.useAbility(player, getAssignedKey(player)) && !LegacyHelper.getLegacyClass(player).getWeaknesses(player) && !player.isInWater()) {
//			boolean success = false;
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
			if(vPlayer.getBoolean(PlayerKeys.IS_VULNERABLE) && vPlayer.getBoolean(PlayerKeys.IS_STARVING)) {
				GeneralUtil.sendDebugMessage(player, new ChatComponentText("Still thirsty").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				cir.cancel();
				return;

			}
			
			
			
			NBTTagCompound nbt = player.getEntityData();
			if(nbt.getInteger("isInCombat") == 1 && MorphHelper.isVampireBeast(player)) {
				cir.cancel();
				nbt.getInteger("isInCombatTimer");
				GeneralUtil.sendDebugMessage(player, new ChatComponentText("Can't change in combat wbile in this form, timer: " + Math.round(nbt.getInteger("isInCombatTimer")/20) + "s").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				return;
			}

			
			if(!MorphHelper.isWolf(player))
				LegendsUtil.ifMorphedTurnHuman(player);
			if(Loader.isModLoaded("witchery")) {
				if(player.worldObj.isRemote) return;
				cir.cancel();
				if(!MorphHelper.isWolf(player)) {
					if(WitcheryUtil.hasBloodPower(player, 0, .05F)) {
						player.worldObj.playSoundAtEntity((Entity)player, "mob.wolf.howl", 1.0F, 1.0F);
						MorphHelper.morph(player, MorphRegistry.wolf);
					}
				} else {
					LegendsUtil.ifMorphedTurnHuman(player);
				}

			}
			if(!(player instanceof EntityPlayerMP)) return;
			if(player.worldObj.isRemote) return;

			EntityPlayerMP playerMP = (EntityPlayerMP) player;
			if(Loader.isModLoaded("Morph")) {
				if (MorphUtil.isPlayerMorphed(playerMP)) {
					MorphUtil.saveMorph(playerMP);
					MorphUtil.forceDemorph(playerMP);
				}
				else if(MorphHelper.isWolf(playerMP)) {
					MorphUtil.reMorphIntoLast(playerMP);
				}
			}
			//			player.worldObj.playSoundAtEntity((Entity)player, "mob.wolf.howl", 2.0F, 1.0F);
			SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 12.0D, "", true, true);
		}				

	}
}
