package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.StatBoostConfig;
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
 * Own wolf morph entirely — stock {@code keyPressed} hard-codes {@code mob.wolf.howl}
 * at 2.0F and stock {@code execute} is empty (Legends 8). Mirror bat mixin pattern.
 */
@Mixin(value = LegacyPowerMorphWolf.class, remap = false)
public abstract class LegacyPowerMorphWolfMixin {

	@Shadow
	public abstract int getAssignedKey(EntityPlayer player);

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void bvWolfKeyPressed(EntityPlayer player, CallbackInfo ci) {
		ci.cancel();
		int key = getAssignedKey(player);
		if (key <= 0) return;
		if (player.worldObj == null || player.worldObj.isRemote) return;

		if (LegacyHelper.getCooldown(player, key) != LegacyHelper.getCooldownMax(player, key)) {
			return;
		}
		if (LegacyHelper.getLegacyClass(player) != null
				&& LegacyHelper.getLegacyClass(player).getWeaknesses(player)) {
			return;
		}
		if (player.isInWater() && !MorphHelper.isWolf(player)) {
			return;
		}

		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
		if (vPlayer != null && vPlayer.getBoolean(PlayerKeys.IS_VULNERABLE)
				&& vPlayer.getBoolean(PlayerKeys.IS_STARVING) && !MorphHelper.isWolf(player)) {
			GeneralUtil.sendDebugMessage(player, new ChatComponentText("Still thirsty")
					.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
			return;
		}

		NBTTagCompound nbt = player.getEntityData();
		if (nbt.getInteger("isInCombat") == 1 && MorphHelper.isVampireBeast(player)
				&& !MorphHelper.isWolf(player)) {
			int combatSec = nbt.getInteger("isInCombatTimer") / 20;
			GeneralUtil.sendDebugMessage(player, new ChatComponentText(
					"Can't change in combat while in this form, timer: " + combatSec + "s")
					.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
			return;
		}

		if (player instanceof EntityPlayerMP && Loader.isModLoaded("Morph")) {
			EntityPlayerMP playerMP = (EntityPlayerMP) player;
			if (MorphUtil.isPlayerMorphed(playerMP) && !MorphHelper.isWolf(player)) {
				MorphUtil.saveMorph(playerMP);
				MorphUtil.forceDemorph(playerMP);
			}
		}

		if (MorphHelper.isWolf(player)) {
			LegendsUtil.ifMorphedTurnHuman(player);
			LegacyHelper.setCooldown(player, key, 0);
			return;
		}

		// Stock refuses morph-from-other-forms except already wolf; allow demorph-first.
		if (MorphHelper.isMorphed(player)) {
			LegendsUtil.ifMorphedTurnHuman(player);
		}

		if (Loader.isModLoaded("witchery") && !WitcheryUtil.hasBloodPower(player, 0, .05F)) {
			return;
		}

		player.worldObj.playSoundAtEntity(player, "mob.wolf.howl",
				StatBoostConfig.wolfMorphVolume, 1.0F);
		MorphHelper.morph(player, MorphRegistry.wolf);
		SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 12.0D, "", true, true);
		LegacyHelper.setCooldown(player, key, 0);
	}

	/** Stock execute is empty; cancel so tick spam cannot re-enter old morph logic. */
	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
	private void bvCancelExecute(EntityPlayer player, CallbackInfo ci) {
		ci.cancel();
	}
}
