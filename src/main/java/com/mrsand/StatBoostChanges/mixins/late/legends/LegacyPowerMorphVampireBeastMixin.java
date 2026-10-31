package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.MorphUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.powers.LegacyPowerMorphVampireBeast;
import com.tihyo.legends.morph.MorphHelper;
import com.tihyo.legends.morph.MorphRegistry;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

@Mixin(value = LegacyPowerMorphVampireBeast.class, remap = false)
public abstract class LegacyPowerMorphVampireBeastMixin {

	@Inject(method = "getCost", at = @At("HEAD"), cancellable = true)
	public void getCost(CallbackInfoReturnable <String>cir) {
		cir.cancel();
		cir.setReturnValue("60");
	}


	@Shadow  
	public abstract int getAssignedKey(EntityPlayer player);

	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
	public void execute(EntityPlayer player, CallbackInfo ci) {
		ci.cancel();
		if (LegacyHelper.useAbility(player, getAssignedKey(player)) && !LegacyHelper.getLegacyClass(player).getWeaknesses(player) && !player.isInWater()) {
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
			if(vPlayer.getBoolean(PlayerKeys.IS_VULNERABLE) && vPlayer.getBoolean(PlayerKeys.IS_STARVING)) {
				GeneralUtil.sendDebugMessage(player, new ChatComponentText("Still thirsty").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				return;
			}
			NBTTagCompound nbt = player.getEntityData();
//			if(nbt.getInteger("isInCombat") == 1) {
//				nbt.getInteger("isInCombatTimer");
//				GeneralUtil.sendDebugMessage(player, new ChatComponentText("Can't change in combat wbile in this form, timer: " + Math.round(nbt.getInteger("isInCombatTimer")/20) + "s").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
//				return;
//			}

			if(player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
				EntityPlayerMP playerMP = (EntityPlayerMP)player;
				if (Loader.isModLoaded("Morph") && playerMP != null) {
					if (MorphUtil.isPlayerMorphed(playerMP)) {
						MorphUtil.saveMorph(playerMP);
						MorphUtil.forceDemorph(playerMP);
					} else if (MorphHelper.isVampireBeast(playerMP)) {
						MorphUtil.reMorphIntoLast(playerMP);
					}
				}
			}

			if(!MorphHelper.isVampireBeast(player))
				LegendsUtil.ifMorphedTurnHuman(player);
			SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 10.0D);
			//			NBTTagCompound nbt = player.getEntityData();

			player.worldObj.playSoundAtEntity((Entity)player, "mob.bat.takeoff", 3.0F, 0.7F);
			MorphHelper.morph(player, MorphRegistry.vampireBeast);

		}
	}
}
