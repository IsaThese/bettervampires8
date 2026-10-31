package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

@Mixin(EntitySpider.class)
public abstract class EntitySpiderMixin extends EntityMob {
	public EntitySpiderMixin(World p_i1738_1_) {
		super(p_i1738_1_);
		// TODO Auto-generated constructor stub
	}
	//Closest player to attack findPlayerToAttack()
//	@Shadow protected World worldObj;
//	@Shadow public abstract float getBrightness(float partialTicks);

	@Inject(method = "func_70782_k", at = @At("HEAD"), cancellable = true)
	protected void changeSpiderIfVampire(CallbackInfoReturnable<Entity> cir) {
		//			EntitySpider self = (EntitySpider)(Object)this;
		cir.cancel();
		float f = this.getBrightness(1.0F);

		if (f < 0.5F) {
			double d0 = 16.0D;
			EntityPlayer player = this.worldObj.getClosestVulnerablePlayerToEntity(this, d0);

			if (player != null) {
//				GeneralUtil.sendDebugMessage(player, "Checking if mob should attack...");
				// If your custom utility says DON'T attack, return null
				if (LegendsUtil.mobAttack(player) || VExtendedPlayer.get(player).getBoolean(PlayerKeys.OTHERFORM)
						|| GeneralUtil.hasBatBlindStealth(player)) {
					cir.setReturnValue(null);
					return;
				}
			}
			cir.setReturnValue(player);
			return;
		}
		cir.setReturnValue(null);

	}


}
