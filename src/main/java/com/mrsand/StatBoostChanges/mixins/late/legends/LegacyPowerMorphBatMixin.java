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
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.powers.LegacyPowerMorphBat;
import com.tihyo.legends.morph.MorphHelper;
import com.tihyo.legends.morph.MorphRegistry;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

@Mixin(value = LegacyPowerMorphBat.class, remap = false)
public abstract class LegacyPowerMorphBatMixin {

	@Shadow public abstract int getAssignedKey(EntityPlayer player);

	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
	public void execute(EntityPlayer player, CallbackInfo ci) {
		if (LegacyHelper.useAbility(player, getAssignedKey(player)) && !LegacyHelper.getLegacyClass(player).getWeaknesses(player) && !player.isInWater()) {
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
			if(vPlayer.getBoolean(PlayerKeys.IS_VULNERABLE) && vPlayer.getBoolean(PlayerKeys.IS_STARVING)) {
				GeneralUtil.sendDebugMessage(player, new ChatComponentText("Still thirsty").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				return;
			}
			NBTTagCompound nbt = player.getEntityData();
			if(nbt.getInteger("isInCombat") == 1 && MorphHelper.isVampireBeast(player)) {
				ci.cancel();
				nbt.getInteger("isInCombatTimer");
				GeneralUtil.sendDebugMessage(player, new ChatComponentText("Can't change in combat while in this form, timer: " + Math.round(nbt.getInteger("isInCombatTimer")/20) + "s").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				return;
			}
			if(!MorphHelper.isBat(player))
				LegendsUtil.ifMorphedTurnHuman(player);
			if(Loader.isModLoaded("witchery")) {
				ci.cancel();
				if(!MorphHelper.isBat(player)) {
					if(WitcheryUtil.decreaseBlood(player, 36, 0F, true)) {
						MorphHelper.morph(player, MorphRegistry.bat);
					}
				} else {
					LegendsUtil.ifMorphedTurnHuman(player);
				}
				player.worldObj.playSoundAtEntity((Entity)player, "mob.bat.takeoff", 3.0F, 1.0F);

			}
			SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 12.0D);

		}
	}

}
