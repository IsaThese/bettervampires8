package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.item.ItemVampireClothes;
import com.emoniph.witchery.util.CreatureUtil;
import com.emoniph.witchery.util.EntityUtil;
import com.emoniph.witchery.util.SoundEffect;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.VampirismUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

@Mixin(value = CreatureUtil.class, remap = false)
public abstract class CreatureUtilMixin {

	@Inject(method = "isInSunlight", at = @At("HEAD"), cancellable = true)
	private static void isInSunlightChange(EntityLivingBase entity, CallbackInfoReturnable<Boolean> cir) {
		if(Loader.isModLoaded("vampirism")) {
			if(VampirismUtil.isInVampireBiome(entity)) {
				cir.setReturnValue(false);
				return;
			}
		}
	}

	@Inject(method = "checkForVampireDeath", at = @At("HEAD"), cancellable = true)
	private static void mistVampireDeathEnd(EntityLivingBase creature, DamageSource source,
			CallbackInfoReturnable<Boolean> cir) {
		cir.cancel();
		boolean dead = false;
		if (creature == null || source == null) cir.setReturnValue(true);;
		if (!(creature instanceof EntityPlayer)) {
			if (source.isFireDamage() || source instanceof EntityUtil.DamageSourceVampireFire) { // func_76347_k
				if (ItemVampireClothes.isExtendedFlameProtectionActive(creature)) {
					dead = (creature.worldObj.rand.nextInt(4) == 0);
				} else if (ItemVampireClothes.isFlameProtectionActive(creature)) {
					dead = (creature.worldObj.rand.nextInt(4) != 0);
				} else {
					dead = true;
				}
			} else if (source instanceof EntityUtil.DamageSourceSunlight) {
				dead = true;
			} else if (creature instanceof EntityPlayer
					&& Witchery.modHooks.canVampireBeKilled((EntityPlayer) creature)) {
				dead = true;
			} else if (source == DamageSource.outOfWorld || source == DamageSource.inWall) { // field_76368_d,
				// field_76380_i
				dead = true;
			} else if (source.getEntity() != null
					&& (CreatureUtil.isWerewolf(source.getEntity()) || CreatureUtil.isVampire(source.getEntity())
							|| source.getEntity() instanceof net.minecraft.entity.boss.IBossDisplayData)) {
				dead = true;
			} else if (CreatureUtil.isWerewolf((Entity) creature, true) && CreatureUtil.isSilverDamage(source)) {
				dead = true;
			}
		} else if (creature instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) creature;
			ExtendedPlayer playerEx = ExtendedPlayer.get(player);
//			GeneralUtil.sendDebugMessage(player, "Death Damage is: " + source.getDamageType());
			if(source == DamageSource.starve) {
				dead = false;
			}
			else if (source == DamageSource.outOfWorld || source == DamageSource.inWall) {
				dead = true;
			} else if (source instanceof EntityUtil.DamageSourceSunlight) {
				dead = true;
			} else if (Witchery.modHooks.canVampireBeKilled(player)) {
				dead = true;
			} else if (playerEx.getBloodPower() == 0 && playerEx.getVampireLevel() == 1) {
				dead = true;
			} else if (LegendsUtil.getWeakness(player) || CreatureUtil.isInSunlight(player)) {
				playerEx.setBloodPower(Math.max(0, 
						playerEx.getBloodPower() - (int)(playerEx.getMaxBloodPower() * (3.0 / 9.0))));
			}
		}

		if (!dead) {
			if (creature instanceof EntityPlayer) {
				EntityPlayer player = (EntityPlayer) creature;
				((EntityPlayer) creature).getFoodStats().addStats(0, 5.0F); // func_71024_bL, func_75113_a
				player.setHealth(player.getMaxHealth());
				ExtendedPlayer playerEx = ExtendedPlayer.get(player);

				//Some i=frames so no gitching.
				player.hurtResistantTime = player.maxHurtResistantTime;
				player.hurtTime = 0;
				player.deathTime = 0;
				player.isDead = false;

				if (playerEx.getBloodPower() > 0) {


					SoundEffect.RANDOM_FIZZ.playOnlyTo(player);
					int bloodPower = playerEx.getBloodPower();
					int maxBlood = playerEx.getMaxBloodPower();

					if(Loader.isModLoaded("legends") && LegendsUtil.checkForVampireDeath(player, bloodPower, maxBlood, CreatureUtil.isInSunlight(creature))) {
						playerEx.setBloodPower((int) Math.round(playerEx.getMaxBloodPower() * .25)); //.333 or .5? //.35 causes it to go back
					}
					else {
						//						playerEx.decreaseBloodPower(300, true);
						playerEx.setBloodPower(Math.max(bloodPower- 300, 0)); //Max logic should already be applied, but ok
					}

					if (source.isExplosion() && creature.worldObj.rand.nextInt(4) == 0) // func_94541_c
						creature.setFire(2); // func_70015_d
					cir.setReturnValue(false);
					return;
				}
				else if(playerEx.getVampireLevel() > 1) {
					WitcheryUtil.decreaseVampireLevel(player);
					WitcheryUtil.decreaseVampireLevel(player);
					WitcheryUtil.decreaseVampireLevel(player);
					cir.setReturnValue(false);
					return;
				}
				if(source == DamageSource.starve) {
					cir.setReturnValue(false);
				}

			} 

		}
		else if (dead && (creature instanceof EntityPlayer)) {
			EntityPlayer player = (EntityPlayer) creature;
			//			WitcheryUtil.decreaseVampireLevel(player);
			//			WitcheryUtil.decreaseVampireLevel(player);
			//			WitcheryUtil.decreaseVampireLevel(player);
			//			GeneralUtil.sendDebugMessage(player, "you died");
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
			vPlayer.setInteger(PlayerKeys.SUSPICION, 0);
			vPlayer.sync();
		}
		cir.setReturnValue(true);
	}
}
// @Inject(method = "checkForVampireDeath", at = @At("HEAD"), cancellable = true
// )
// private static void dieIfNoBlood(EntityLivingBase creature, DamageSource
// source, CallbackInfoReturnable<Boolean> cir) {
// boolean dead = cir.getReturnValueZ();
// if (!dead) {
// if (creature instanceof EntityPlayer) {
// EntityPlayer player = (EntityPlayer) creature;
// ExtendedPlayer playerEx = ExtendedPlayer.get(player);
//
// if (playerEx.getBloodPower() == 0) {
// if(source == DamageSource.starve) {
// cir.setReturnValue(false);
// cir.cancel();
// }
// else if (source != DamageSource.starve){
// cir.setReturnValue(true);
// }
// }
//
//
// }
// }
// }

// @Inject(method = "checkForVampireDeath", at = @At("HEAD"), cancellable = true
// )
// private static void dayVampireDeath(EntityLivingBase creature, DamageSource
// source, CallbackInfoReturnable<Boolean> cir) {
// boolean dead = cir.getReturnValueZ();
// if (creature instanceof EntityPlayer) {
// if (CreatureUtil.isInSunlight(creature) | creature.worldObj.isDaytime()){
// PowersHandler.mistForm((EntityPlayer) creature, 20);
// cir.setReturnValue(true);
// cir.cancel();
// }
//
// }
// }

