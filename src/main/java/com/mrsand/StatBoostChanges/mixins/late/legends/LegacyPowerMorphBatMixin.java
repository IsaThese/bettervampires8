package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.BatFormDebug;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.entity.player.skills.Skills;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.powers.LegacyPowerMorphBat;
import com.tihyo.legends.morph.MorphHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

/**
 * Own bat toggle entirely — stock keyPressed + our old execute both morphing caused
 * enter-then-immediate-exit on one press.
 */
@Mixin(value = LegacyPowerMorphBat.class, remap = false)
public abstract class LegacyPowerMorphBatMixin {

	@Shadow public abstract int getAssignedKey(EntityPlayer player);

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void bvBatKeyPressed(EntityPlayer player, CallbackInfo ci) {
		ci.cancel();
		int key = getAssignedKey(player);
		if (key <= 0) return;
		if (player.worldObj.isRemote) return;

		BatFormDebug.log(player, "keyPressed", "begin");

		if (!MorphHelper.isBat(player)) {
			if (player.isInWater()) {
				BatFormDebug.log(player, "keyPressed", "water");
				return;
			}
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
			if (vPlayer != null && vPlayer.getBoolean(PlayerKeys.IS_VULNERABLE)
					&& vPlayer.getBoolean(PlayerKeys.IS_STARVING)) {
				GeneralUtil.sendDebugMessage(player, new ChatComponentText("Still thirsty")
						.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				return;
			}
			NBTTagCompound nbt = player.getEntityData();
			if (nbt.getInteger("isInCombat") == 1 && MorphHelper.isVampireBeast(player)) {
				GeneralUtil.sendDebugMessage(player, new ChatComponentText(
						"Can't change in combat while in this form, timer: "
								+ Math.round(nbt.getInteger("isInCombatTimer") / 20) + "s")
						.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				return;
			}
		}

		// Same controller as Witchery pie / BatSkill UI.
		if (Skills.batMode != null) {
			VampirePlayer.get(player).onSkillToggled(Skills.batMode.getId());
		}
		LegacyHelper.setCooldown(player, key, LegacyHelper.getCooldownMax(player, key));
		BatFormDebug.log(player, "keyPressed", "done isBat=" + MorphHelper.isBat(player));
	}

	/** Prevent execute from morphing again after keyPressed (was the double-toggle). */
	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
	public void execute(EntityPlayer player, CallbackInfo ci) {
		ci.cancel();
	}
}
