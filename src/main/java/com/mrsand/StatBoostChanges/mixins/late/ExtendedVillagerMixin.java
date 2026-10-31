package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.common.ExtendedVillager;
import com.emoniph.witchery.item.ItemVampireClothes;
import com.emoniph.witchery.util.TransformCreature;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.common.management.VExtendedVillager;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumChatFormatting;

@Mixin(value = ExtendedVillager.class, remap = false)
public abstract class ExtendedVillagerMixin {

	@Shadow
	private EntityVillager villager;
	@Shadow
	private int blood;

	@Shadow
	public abstract boolean isSleeping();

	@Shadow
	public abstract void setBlood(int blood);

	@Inject(method = "takeBlood", at = @At("HEAD"), cancellable = true)

	// This calculates the blood and returns the value a vampire takes on a villager
	// and sets the villager's blood
	private void takeBlood(int quantity, EntityLivingBase player, CallbackInfoReturnable<Integer> cir) {
		if (player instanceof EntityPlayer) {
			cir.cancel();
			if(this.villager.getHealth() == 0) {
				cir.setReturnValue(0);
				return;
			}

			EntityPlayer playerEnt = (EntityPlayer) player;
			ExtendedPlayer playerEx = ExtendedPlayer.get(playerEnt);
			player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 20 * 3, 1, true));
			int drinkAmount = ItemVampireClothes.isDrinkBoostActive(player) ? 15 : 10;
			int take = (playerEx.getCreatureType() == TransformCreature.NONE) ? drinkAmount : 2;
			int inQuantity = take;
			int level = playerEx.getVampireLevel();

			if (level < 9 && level != 2) {
				inQuantity = 150; // 130 if half works
			} else if (level == 10) {
				inQuantity = 264; // 10% of villager dying
			}

			float multiplier = 1.0F + (float) Math.random(); // Math.random() returns double [0.0,1.0), so this is
			// [1.0,2.0)
			quantity = (int) (inQuantity * multiplier);
			if(level < 9) {
				quantity = Math.min(quantity, 250);
			}
			boolean starve = VExtendedPlayer.get(playerEnt).getBoolean(PlayerKeys.IS_STARVING);
			boolean caged = VExtendedVillager.villagerIsInCage(villager);
			boolean ifCagedKill = false;
			PotionEffect potionEffect = this.villager.getActivePotionEffect(Witchery.Potions.PARALYSED); // func_70660_b
			boolean isKnockedOut = WitcheryUtil.isKnockedOut(villager); // func_76458_c


			if (starve || level == 1) {
				quantity = 1500;
			}
			// was == 9
			else if (caged) {
				quantity = take;
			}

			if (!isKnockedOut && !starve) {
				double chance = level*.05F + .5F;
				if(player.worldObj.rand.nextFloat() < chance || caged) {
					quantity = 1500;
					ifCagedKill = true;
				}
				else {
					quantity = take;
				}
			}
			int remainder = Math.max(this.blood - quantity, 0);
			int taken = this.blood - remainder;
			if (WitcheryUtil.hasEmptyWitcheryGoblet((EntityPlayer) player) && isKnockedOut && !caged) {
				ItemStack stack = WitcheryUtil.getWitcheryGoblet(playerEnt);
				GeneralUtil.sendDebugMessage(playerEnt, new ChatComponentText("Filled Goblet").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED) ));
				WitcheryUtil.setWitcheryGoblet(stack, playerEnt, "Human");
				playerEx.setBloodPower(playerEx.getMaxBloodPower() - taken);
				if (playerEnt instanceof net.minecraft.entity.player.EntityPlayerMP) {
					((net.minecraft.entity.player.EntityPlayerMP) playerEnt).sendContainerToPlayer(playerEnt.inventoryContainer);
				}
			}

			setBlood(remainder);

			// If blood is low (below 250)
			if (blood == 0 && caged) {
			}



			if (blood == 0 && (!caged || ifCagedKill)) {

				this.villager.setHealth(0.0F);
				this.villager.onDeath(DamageSource.causePlayerDamage((EntityPlayer) player));
				playerEx.increaseBloodPower(85);
				double chance = Math.random();
				//					if (player.worldObj.rand.nextInt(7) < 3) {
				//						GeneralUtil.givePlayerItem(playerEnt, new ItemStack(Items.spawn_egg, 1, 120));
				//					}
//				if (GeneralUtil.checkForSuspicionWitnesses(playerEnt, 0, 128)) {
//					WitcheryUtil.spawnHunter(playerEnt, true);
//				} else {
//					GeneralUtil.increaseSuspicion(playerEnt, 1500);
//				}
//				GeneralUtil.villagerDied(playerEnt);
			}
			else if (blood < 250 && isKnockedOut) {
				if(level != 10)
					this.villager.attackEntityFrom(
							(DamageSource) new EntityDamageSource(DamageSource.magic.getDamageType(), (Entity) player),
							1.3F);



				long percent = taken / 500;
				int increase = taken;
				NBTTagCompound nbt = this.villager.getEntityData();

				if (!caged) {
					if (level == 10)
						increase = playerEx.getMaxBloodPower();
					else {
						increase = (int) Math.round(playerEx.getMaxBloodPower() * .8);
					}
//					if (this.villager.getHealth() != 0) {
//						int bites = nbt.getInteger("bitesLeft") == 0 ? 2 : nbt.getInteger("bitesLeft");
//						bites--;
//						nbt.setInteger("bitesLeft", bites);
//						if (bites == 0) {
//							nbt.removeTag("bitesLeft");
//							this.villager.setHealth(0.0F);
//							this.villager.onDeath(DamageSource.causePlayerDamage((EntityPlayer) player));
//							if (GeneralUtil.checkForSuspicionWitnesses(playerEnt, 0, 128))
//								GeneralUtil.spawnHunter(playerEnt);
//							else {
//								GeneralUtil.increaseSuspicion(playerEnt, 1000);
//							}
//							GeneralUtil.villagerDied(playerEnt);
//
//						}
//						playerEnt.addChatComponentMessage(
//								new ChatComponentText("They can survive " + bites + " more times")
//								.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
//						// GeneralUtil.sendDebugMessage(playerEnt, "They can only survive " + bites + "
//						// more times");
//
//					}
				}

				cir.setReturnValue(increase);
				return;

			}
			cir.setReturnValue(taken);
		}
	}

	@Inject(method = "giveBlood", at = @At("HEAD"), cancellable = true)
	private void giveBiteChance(int quantity, CallbackInfo ci) {
//		NBTTagCompound nbt = this.villager.getEntityData();
//		if (nbt.getInteger("bitesLeft") != 0) {
//			int counter = nbt.getInteger("biteTimer") == 0 ? 2 : nbt.getInteger("biteTimer");
//			counter--;
//			nbt.setInteger("biteTimer", counter);
//			if (counter == 1) {
//				nbt.removeTag("biteTimer");
//				nbt.setInteger("bitesLeft", (int) Math.min(nbt.getInteger("bitesLeft") + 1, 2));
//			}
//		}

	}
}
