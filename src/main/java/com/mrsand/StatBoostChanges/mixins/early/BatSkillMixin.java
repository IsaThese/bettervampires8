package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;

import de.teamlapen.vampirism.entity.player.skills.BatSkill;
import de.teamlapen.vampirism.entity.player.skills.DefaultSkill;
import de.teamlapen.vampirism.entity.player.skills.ILastingSkill;

@Mixin(BatSkill.class)
public abstract class BatSkillMixin extends DefaultSkill implements ILastingSkill {

//
//	//	@Shadow public UUID healthModifierUUID;
//	public final UUID healthModifierUUID = UUID.fromString("4392fccb-4bfd-4290-b2e6-5cc91429053c");
//	//OR SET MODIFIER
//	
//	
//	@Inject(method = "setModifier", at = @At("HEAD"), cancellable = true)
//	private void setNewModifier(EntityPlayer player, boolean enabled, CallbackInfo ci) {
//		GeneralUtil.sendDebugMessage(player, "custom set modifer");
//		ci.cancel();
//		if (enabled) {
//			IAttributeInstance health = player.getEntityAttribute(SharedMonsterAttributes.maxHealth);
//			if (health.getModifier(this.healthModifierUUID) == null)
//				health.applyModifier(
//						(new AttributeModifier(this.healthModifierUUID, "Bat Health Reduction", -0.9D, 2))
//						.setSaved(false)
//						);
//			//	        player.capabilities.allowFlying = true;
//			//	        player.capabilities.isFlying = true;
//			player.sendPlayerAbilities();
//		} else {
//			IAttributeInstance health = player.getEntityAttribute(SharedMonsterAttributes.maxHealth);
//			PlayerModifiers.rmMod(health, this.healthModifierUUID);
//			if (!player.capabilities.isCreativeMode)
//				player.capabilities.allowFlying = false;
//			//	        player.capabilities.isFlying = false;
//			player.sendPlayerAbilities();
//		}
//	}
}

