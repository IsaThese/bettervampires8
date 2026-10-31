package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emoniph.witchery.common.ExtendedPlayer;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.PlayerDataUtil;
import com.mrsand.StatBoostChanges.util.VisualUtil;
import com.tihyo.horror.legacy.LegacyVampire;
import com.tihyo.legends.abilities.Ability;
import com.tihyo.legends.abilities.AbilityHelper;
import com.tihyo.legends.abilities.LegendsAbilities;
import com.tihyo.legends.abilities.LegendsImmunities;
import com.tihyo.legends.effects.LegendsEffects;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

@Mixin(value = LegacyVampire.class, remap = false)
public abstract class LegacyVampireMixin {

	@Shadow public abstract boolean getWeaknesses(EntityPlayer player);

	@Inject(method = "stats", at = @At("HEAD"), cancellable = true)
	private void changeStats(EntityPlayer player, int rank, CallbackInfo cir) {
		cir.cancel();
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);

		if (!AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.bloodAmount)) {
			PlayerHelper.setBloodMeter(player, 0);
		}
		if(AbilityHelper.isAbilityActive(player, Ability.mistForm)) {
			LegendsAbilities.flight2(player, 3);
			//			LegendsAbilities.flight3(player, 3);
			//			LegendsAbilities.flight4(player, 3);
			//			LegendsAbilities.flightLegacy(player, rank);
			//			LegendsAbilities.flight1(player, 3);
			//PowersHandler.intangible(player, 1);
		}

		if (MorphHelper.isVampireBeast(player) && !PlayerHelper.isClimbing(player) && !PlayerHelper.isClimbingUpsidedown(player)) {
			LegendsAbilities.flightLegacy(player, 3);
			if(!(player.isPotionActive(Potion.moveSpeed) || player.isPotionActive(Potion.moveSlowdown)))
				LegendsAbilities.speed(player, 8, 8);
		}

		if (!MorphHelper.isMorphed((EntityLivingBase)player)) {
			LegendsAbilities.stealth(player);
		}

		if (LegacyHelper.getLegacyClass(player).getWeaknesses(player)) {
			vPlayer.setBoolean(PlayerKeys.HYPNOTIZING, false);
			if(!vPlayer.getBoolean(PlayerKeys.IS_STARVING))
				VisualUtil.changeForm(player, false);
			if (MorphHelper.isMorphed((EntityLivingBase)player)) {
				PlayerHelper.setPlayerInfo(player, 
						PlayerHelper.getEyeType(player), 
						PlayerHelper.getFlightStyle(player), 
						PlayerHelper.getBeamColor(player), 
						PlayerHelper.getLegacyClawNumber(player), 
						PlayerHelper.getObsessionName(player), 
						PlayerHelper.isPatron(player), 
						PlayerHelper.getTransparencyAmount(player), 
						PlayerHelper.getSpeedsterTrailColor(player), 
						PlayerHelper.getMarkCalled(player), 
						false, 
						PlayerHelper.getSymbioteType(player), 
						PlayerHelper.isInKeyMode(player)
						);
			}
		}
		if (LegacyHelper.getRank(player) >=1 ) { // Changes stats for all ranks
			//player.addChatMessage(new ChatComponentText("You are greater than rank 0"));

			if (LegacyHelper.isActive(player)
					&& LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire 
					&& !LegacyHelper.getLegacyClass(player).getWeaknesses(player)) {

				ItemStack itemHeld = player.getHeldItem();
				if(itemHeld == null || !PlayerDataUtil.isWeapon(itemHeld)) {
					//player.addChatComponentMessage(new ChatComponentText("Your blood power is " + playerEx.getBloodPower()));

					//        				LegendsAbilities.strength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) 
					//        					? ((MorphHelper.isVampireBeast(player) ? 20 : (MorphHelper.isWolf(player) 
					//        					? ((14 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (14 + PlayerHelper.getBloodAmount(player) / 20))) / 2) 
					//        					: (MorphHelper.isVampireBeast(player) ? 20 : (MorphHelper.isWolf(player) 
					//        					? ((14 + PlayerHelper.getBloodAmount(player) / 20) / 2) 
					//        					: (14 + PlayerHelper.getBloodAmount(player) / 20))));
					if(playerEx.getBloodPower() < 600 && playerEx.getBloodPower() >= 1) {

						LegendsAbilities.strength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) 
								? ((MorphHelper.isVampireBeast(player) ? 8 : (MorphHelper.isWolf(player) 
										? ((5 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (5 + PlayerHelper.getBloodAmount(player) / 20))) / 2) 
										: (MorphHelper.isVampireBeast(player) ? 8 : (MorphHelper.isWolf(player) 
												? ((5 + PlayerHelper.getBloodAmount(player) / 20) / 2) 
														: (5 + PlayerHelper.getBloodAmount(player) / 20))));

					}
					if(playerEx.getBloodPower() < 1200 && playerEx.getBloodPower() >= 600 ) {

						LegendsAbilities.strength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) 
								? ((MorphHelper.isVampireBeast(player) ? 9 : (MorphHelper.isWolf(player) 
										? ((6 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (6 + PlayerHelper.getBloodAmount(player) / 20))) / 2) 
										: (MorphHelper.isVampireBeast(player) ? 9 : (MorphHelper.isWolf(player) 
												? ((6 + PlayerHelper.getBloodAmount(player) / 20) / 2) 
														: (6 + PlayerHelper.getBloodAmount(player) / 20))));
					}
					if(playerEx.getBloodPower() < 1200 && playerEx.getBloodPower() >= 900 ) {

						LegendsAbilities.strength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) 
								? ((MorphHelper.isVampireBeast(player) ? 11 : (MorphHelper.isWolf(player) 
										? ((8 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (8 + PlayerHelper.getBloodAmount(player) / 20))) / 2) 
										: (MorphHelper.isVampireBeast(player) ? 11 : (MorphHelper.isWolf(player) 
												? ((8 + PlayerHelper.getBloodAmount(player) / 20) / 2) 
														: (8 + PlayerHelper.getBloodAmount(player) / 20))));
					}
					if(playerEx.getBloodPower() < 1500 && playerEx.getBloodPower() >= 1200 ) {

						LegendsAbilities.strength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) 
								? ((MorphHelper.isVampireBeast(player) ? 13 : (MorphHelper.isWolf(player) 
										? ((9 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (10 + PlayerHelper.getBloodAmount(player) / 20))) / 2) 
										: (MorphHelper.isVampireBeast(player) ? 13 : (MorphHelper.isWolf(player) 
												? ((9 + PlayerHelper.getBloodAmount(player) / 20) / 2) 
														: (9 + PlayerHelper.getBloodAmount(player) / 20))));
					}
					if(playerEx.getBloodPower() < 1800 && playerEx.getBloodPower() >= 1500 ) {

						LegendsAbilities.strength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) 
								? ((MorphHelper.isVampireBeast(player) ? 15 : (MorphHelper.isWolf(player) 
										? ((11 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (11 + PlayerHelper.getBloodAmount(player) / 20))) / 2) 
										: (MorphHelper.isVampireBeast(player) ? 15 : (MorphHelper.isWolf(player) 
												? ((11 + PlayerHelper.getBloodAmount(player) / 20) / 2) 
														: (11 + PlayerHelper.getBloodAmount(player) / 20))));
					}
					if(playerEx.getBloodPower() < 2400 && playerEx.getBloodPower() >= 1800 ) {

						LegendsAbilities.strength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) 
								? ((MorphHelper.isVampireBeast(player) ? 17 : (MorphHelper.isWolf(player) 
										? ((12 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (13 + PlayerHelper.getBloodAmount(player) / 20))) / 2) 
										: (MorphHelper.isVampireBeast(player) ? 17 : (MorphHelper.isWolf(player) 
												? ((12 + PlayerHelper.getBloodAmount(player) / 20) / 2) 
														: (12 + PlayerHelper.getBloodAmount(player) / 20))));
					}
					if(playerEx.getBloodPower() < 2700 && playerEx.getBloodPower() >= 2400 ) {

						LegendsAbilities.strength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) 
								? ((MorphHelper.isVampireBeast(player) ? 19 : (MorphHelper.isWolf(player) 
										? ((13 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (13 + PlayerHelper.getBloodAmount(player) / 20))) / 2) 
										: (MorphHelper.isVampireBeast(player) ? 19 : (MorphHelper.isWolf(player) 
												? ((13 + PlayerHelper.getBloodAmount(player) / 20) / 2) 
														: (13 + PlayerHelper.getBloodAmount(player) / 20))));
					}
					if(playerEx.getBloodPower() < 3300 && playerEx.getBloodPower() >= 2700 ) {

						LegendsAbilities.strength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) 
								? ((MorphHelper.isVampireBeast(player) ? 20 : (MorphHelper.isWolf(player) 
										? ((14 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (13 + PlayerHelper.getBloodAmount(player) / 20))) / 2) 
										: (MorphHelper.isVampireBeast(player) ? 20 : (MorphHelper.isWolf(player) 
												? ((14 + PlayerHelper.getBloodAmount(player) / 20) / 2) 
														: (14 + PlayerHelper.getBloodAmount(player) / 20))));
					}
					if(AbilityHelper.isAbilityActive(player, Ability.strength)) {
						AbilityHelper.getEffect(player, rank);
					}


					//LegendsAbilities.strength(player, 14);
					//bilityHelper.addEffect((EntityLivingBase)player, Ability.strength.id, 10, 14); 

				}       			
				//AbilityHelper.addEffectAndSync((EntityLivingBase)player, Ability.strength, 10, 14);
				Boolean wVampire = false;
				if(Loader.isModLoaded("witchery")) {
					if(ExtendedPlayer.get(player).getVampireLevel() > 0) {
						wVampire = true;
					}
				}

				if(!wVampire) {
					LegendsAbilities.speed(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) ?
							((MorphHelper.isVampireBeast(player) ? 15 : 
								(5 + PlayerHelper.getBloodAmount(player) / 50 + PlayerHelper.getBloodLustSpeed(player))) / 2) 
							: (MorphHelper.isVampireBeast(player) 
									? 15 : (5 + PlayerHelper.getBloodAmount(player) / 50 + PlayerHelper.getBloodLustSpeed(player))), 
							-1 + PlayerHelper.getBloodLustSpeed(player));
				}

				if (!MorphHelper.isWolf(player))
					LegendsAbilities.acrobatics(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) ? ((MorphHelper.isVampireBeast(player) ? 6 : 4) / 2) : (MorphHelper.isVampireBeast(player) ? 6 : 4));



				//LegendsAbilities.fortitude(player, 0);
				if(playerEx.getBloodPower() >= 500) {
					//LegendsAbilities.fortitude(player, 1);
				}
				LegendsAbilities.mentalDefense(player, 3);
				LegendsAbilities.regeneration(player, 0);
				if(!playerEx.isVampire()) {

					LegendsAbilities.strength(player, player.isPotionActive(LegendsEffects.werewolfVenom.id) 
							? ((MorphHelper.isVampireBeast(player) ? 20 : (MorphHelper.isWolf(player) 
									? ((14 + PlayerHelper.getBloodAmount(player) / 20) / 2) : (13 + PlayerHelper.getBloodAmount(player) / 20))) / 2) 
									: (MorphHelper.isVampireBeast(player) ? 20 : (MorphHelper.isWolf(player) 
											? ((14 + PlayerHelper.getBloodAmount(player) / 20) / 2) 
													: (14 + PlayerHelper.getBloodAmount(player) / 20))));	
					PowersHandler.nightVision(player, 220, 2); //Disable nightvision
				}
				LegendsAbilities.terror(player, 14); //Enable flying near vampire mobs (Maybe creatre ur own!?)
				LegendsAbilities.composure(player);
				LegendsImmunities.poison(player);
				LegendsImmunities.wither(player);
				LegendsImmunities.fallDamage(player);
			}
			if(MorphHelper.isWolf(player)) {
				LegendsAbilities.stepAssist(player, 1);
				//				if(!(player.isPotionActive(Potion.moveSpeed) || player.isPotionActive(Potion.moveSlowdown)))
				LegendsAbilities.speed(player, 17, 17);
				LegendsAbilities.acrobatics(player, 5);

			}
			

			cir.cancel();
			//}
		}cir.cancel();
	} //
}//cir.cancel();
//}

