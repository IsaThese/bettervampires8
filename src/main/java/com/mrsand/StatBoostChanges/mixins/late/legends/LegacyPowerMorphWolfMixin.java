package com.mrsand.StatBoostChanges.mixins.late.legends;

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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

/**
 * Wolf power toggle/cooldown only. FP wolf arm is drawn by {@code MorphWolfMixin}
 * (LegacyPowerMorphWolf has no {@code renderRightHand}).
 */
@Mixin(value = LegacyPowerMorphWolf.class, remap = false)
public abstract class LegacyPowerMorphWolfMixin {

	@Shadow
	public abstract int getAssignedKey(EntityPlayer player);

	/**
	 * Stock keyPressed calls {@code setCooldown(key, 200)} which sets max=200 and current=0
	 * (ready only when current == max). Force max=0 so current==max immediately.
	 */
	@Inject(method = "keyPressed", at = @At("RETURN"))
	private void clearWolfCooldown(EntityPlayer player, CallbackInfo ci) {
		int key = getAssignedKey(player);
		if (key <= 0 || player.worldObj.isRemote) return;
		LegacyHelper.setCooldown(player, key, 0);
	}

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
				int combatSec = nbt.getInteger("isInCombatTimer") / 20;
				GeneralUtil.sendDebugMessage(player, new ChatComponentText("Can't change in combat while in this form, timer: " + combatSec + "s").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				return;
			}

			
			if(!MorphHelper.isWolf(player))
				LegendsUtil.ifMorphedTurnHuman(player);
			if(Loader.isModLoaded("witchery")) {
				if(player.worldObj.isRemote) return;
				cir.cancel();
				if(!MorphHelper.isWolf(player)) {
					if(WitcheryUtil.hasBloodPower(player, 0, .05F)) {
						player.worldObj.playSoundAtEntity(player, "mob.wolf.howl", 1.0F, 1.0F);
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
