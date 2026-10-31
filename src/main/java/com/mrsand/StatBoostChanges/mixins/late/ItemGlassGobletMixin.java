package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.common.ExtendedVillager;
import com.emoniph.witchery.entity.EntityVampire;
import com.emoniph.witchery.entity.EntityVillageGuard;
import com.emoniph.witchery.item.ItemGlassGoblet;
import com.emoniph.witchery.item.ItemGlassGoblet.BloodSource;
import com.emoniph.witchery.util.ChatUtil;
import com.emoniph.witchery.util.Config;
import com.emoniph.witchery.util.CreatureUtil;
import com.emoniph.witchery.util.ParticleEffect;
import com.emoniph.witchery.util.SoundEffect;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityInteractEvent;


@Mixin(value = ItemGlassGoblet.class, remap = false)
public abstract class ItemGlassGobletMixin {


	@Shadow protected abstract boolean hasBlood(ItemStack stack); 
	@Shadow protected abstract boolean hasBloodType(ItemStack stack, BloodSource source); 
	@Shadow public abstract void setBloodOwner(ItemStack stack, BloodSource source);  
	@Shadow public abstract void setBloodOwner(ItemStack stack, EntityPlayer player);  

	@Inject(method = "func_77654_b", at = @At("HEAD"), cancellable = true) //Called onEaten
	public void onEatenHumanBlood(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) { 
		cir.cancel();
		// Client also finishes item use; only the server may mutate NBT / blood / stacks
		if (world.isRemote) {
			cir.setReturnValue(stack);
			return;
		}

		ExtendedPlayer playerEx = ExtendedPlayer.get(player);

		if (!hasBlood(stack)) {
			// --- Fill Goblet ---
			if (playerEx.getVampireLevel() >= 9 && (playerEx.getBloodPower() >= 900 || player.capabilities.isCreativeMode || player.worldObj.difficultySetting == EnumDifficulty.PEACEFUL)) {
				if (!WitcheryUtil.tryConsumeGobletAction(player)) {
					cir.setReturnValue(stack);
					return;
				}
				ItemStack used = WitcheryUtil.splitOneGobletForUse(stack);
				playerEx.decreaseBloodPower(900, true);
				ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, world, player.posX, player.posY + (player.height * 0.85F), player.posZ, 0.8D, 0.3D, 16); 
				setBloodOwner(used, player);
				WitcheryUtil.returnSplitGoblet(player, stack, used);
			} else {
				if (playerEx.isVampire() && playerEx.getVampireLevel() < 9) {
					ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)player, "item.witchery:glassgoblet.notenoughblood", new Object[0]);
				} else if (playerEx.getBloodPower() < 900) {
					ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)player, "item.witchery:glassgoblet.nothighenoughlevel", new Object[0]);
				} else {
					ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)player, "item.witchery:glassgoblet.nothinghappens", new Object[0]);
				}
				SoundEffect.NOTE_SNARE.playOnlyTo(player);
			}
			cir.setReturnValue(stack.stackSize > 0 ? stack : null);
			return;
		}

		// --- Drink Blood ---
		if (!playerEx.isVampire() && !hasBloodType(stack, BloodSource.CHICKEN) && !WitcheryUtil.hasBloodType("Animal", stack) && !WitcheryUtil.hasBloodType("Human", stack)
				&& !(Config.instance()).allowVampireWolfHybrids && playerEx.getWerewolfLevel() > 0) {
			ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)player, "witchery.rite.wolfcurse.hybridsnotallow", new Object[0]);
			cir.setReturnValue(stack);
			return;
		}
		if (!WitcheryUtil.tryConsumeGobletAction(player)) {
			cir.setReturnValue(stack);
			return;
		}

		ItemStack used = WitcheryUtil.splitOneGobletForUse(stack);
		world.playSoundAtEntity((Entity)player, "random.burp", 0.5F, world.rand.nextFloat() * 0.1F + 0.9F); 

		if (!playerEx.isVampire() && !hasBloodType(used, BloodSource.CHICKEN) && !WitcheryUtil.hasBloodType("Animal", used) && !WitcheryUtil.hasBloodType("Human", used)) {
			if (playerEx.getBloodPower() == 0 && !CreatureUtil.isInSunlight((EntityLivingBase)player)) {
				playerEx.increaseVampireLevel();
				ParticleEffect.SMOKE.send(SoundEffect.WITCHERY_RANDOM_POOF, (Entity)player, 0.8D, 1.5D, 16);
			} else {
				player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 10000, 1));
				player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 10000, 1));
			} 
		} 

		if (playerEx.isVampire()) {
			if (WitcheryUtil.hasBloodType("__Human", used)) {
				if (playerEx.getVampireLevel() == 10)
					playerEx.increaseBloodPower((int)Math.round(playerEx.getMaxBloodPower()*.7*.333333));
				else if (playerEx.getVampireLevel() != 9)
					playerEx.increaseBloodPower((int)Math.round(playerEx.getMaxBloodPower()*.5*.333333));
				else if (playerEx.getVampireLevel() == 9)
					playerEx.increaseBloodPower((int)Math.round(2*.333333));

				player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 100, 0));
				player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 1200, 0));
			}
			else if (getBloodOwner(used, world) == player) {
				playerEx.increaseBloodPower((int)Math.round(playerEx.getMaxBloodPower()*.25*.333333));
			}
			else if (getBloodOwner(used, world) != player) {
				playerEx.increaseBloodPower((int)Math.round(playerEx.getMaxBloodPower()*.65*.33333));
				player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 200, 1));
				player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 2400, 1));
			}
		}

		if (used.hasTagCompound() && used.getTagCompound().hasKey("BloodUseCounter")) {
			NBTTagCompound nbt = used.getTagCompound();
			int uses = nbt.getInteger("BloodUseCounter") - 1;

			if (uses > 0) {
				nbt.setInteger("BloodUseCounter", uses);

				String uuidS = nbt.hasKey("WITCBloodUUID") ? nbt.getString("WITCBloodUUID") : "";
				if ("__Human".equals(uuidS)) {
					nbt.setString("WITCBloodName", "Human (" + uses + "/3 Uses)");
				} else {
					String playerName = nbt.hasKey("WITCPlayerName") ? nbt.getString("WITCPlayerName") : "";
					nbt.setString("WITCBloodName", playerName + " (" + uses + "/3 Uses)");
				}
			} else {
				setBloodOwner(used, BloodSource.EMPTY); 
			}
		} else {
			setBloodOwner(used, BloodSource.EMPTY);
		}

		WitcheryUtil.returnSplitGoblet(player, stack, used);
		cir.setReturnValue(stack.stackSize > 0 ? stack : null);
	}




	@Shadow protected abstract boolean isCoffinNear(World world, Entity entity, int radius);
	@Shadow protected abstract void convertToVampire(EntityLiving entity);
	@Shadow protected abstract EntityPlayer getBloodOwner(ItemStack stack, World world);

	@Overwrite
	//	@Inject(method = "tryConvertToVampire", at = @At("HEAD"), cancellable = true)
	private boolean tryConvertToVampire(EntityLiving target, int blood, EntityPlayer player, ItemStack stack) {
		// Check for a Paralysis PotionEffect on the target
		//		PotionEffect effect = target.getActivePotionEffect(Witchery.Potions.PARALYSED);
		//	    if (effect != null && effect.getAmplifier() >= 5) {
		//	      if (blood == 0) {
		//	        if (isCoffinNear(player.worldObj, (Entity)target, 4)) {
		//	          convertToVampire(target);
		//	          ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		//	          if (playerEx.getVampireLevel() == 9 && playerEx.canIncreaseVampireLevel() && 
		//	            getBloodOwner(stack, player.worldObj) == player)
		//	            playerEx.increaseVampireLevel(); 
		//	          cir.setReturnValue(true);
		//	        } 
		//	        ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)player, "item.witchery:glassgoblet.nocoffinnear", new Object[0]);
		//	      } else {
		//	        ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)player, "item.witchery:glassgoblet.targetnotdrained", new Object[0]);
		//	      } 
		//	    } else {
		//	      ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)player, "item.witchery:glassgoblet.targetnottransfixed", new Object[0]);
		//	    } 
		//	    cir.setReturnValue(false);
		//	  }
		PotionEffect effect = target.getActivePotionEffect(Witchery.Potions.PARALYSED); // func_70660_b
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		if (effect != null && effect.getAmplifier() >= 5) { // func_76458_c
			if (blood < 250) {
				if ( //isCoffinNear(player.worldObj, (Entity)target, 4) || 
						(playerEx.getVampireLevel() == 9 && blood == 0)
						|| playerEx.getVampireLevel() == 10) { // field_70170_p
					convertToVampire(target);
					if (playerEx.getVampireLevel() == 9 && 
							WitcheryUtil.isOwner(stack, player)
							) {
						playerEx.increaseVampireLevel(); 
					}
					// Quest advancement if player is level 9 and blood was their own 
					//					cir.setReturnValue(true);
					return true;

				}
				else { 
					ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)player, "item.witchery:glassgoblet.nocoffinnear", new Object[0]);
				}
			} else {
				ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)player, "item.witchery:glassgoblet.targetnotdrained", new Object[0]);
			} 
		} else {
			ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)player, "item.witchery:glassgoblet.targetnottransfixed", new Object[0]);
		} 
		//		cir.setReturnValue(false);
		return false;
	}

	//	@Shadow protected abstract boolean tryConvertToVampire(EntityLiving target, int blood, EntityPlayer player, ItemStack stack);

	@Inject(method = "onEntityInteract", at = @At("HEAD"), cancellable = true)
	public void onEntityInteract(World world, EntityPlayer player, ItemStack stack, EntityInteractEvent event, CallbackInfo cir) {
		// Check if on server, goblet has blood, and target is not a Werewolf or Vampire
		cir.cancel();
		//		if (!event.entityPlayer.worldObj.isRemote && hasBlood(stack) && !CreatureUtil.isWerewolf(event.target, true)) {
		//			boolean success = false;
		//			if (getBloodOwner(stack, player.worldObj) == player) {
		//
		//				// Duration is in ticks (e.g., 200 = 10 seconds)
		//				// Amplifier 2 = Regeneration III (starts from 0)
		//				EntityLivingBase entity2 = (EntityLivingBase) event.target;
		//				entity2.addPotionEffect(new PotionEffect(Potion.regeneration.id, 200, 2));
		//				success = true;
		//
		//				//Witchery Vampire
		//				if (event.target instanceof EntityVampire) {
		//					EntityVampire vamp = (EntityVampire) event.target;
		//					vamp.heal(20.0F); // Heal 4 hearts directly
		//					GeneralUtil.enslaveMob((EntityLiving)vamp, player);
		//					success = true;
		//
		//				}
		//				if (event.target instanceof EntityMob && !(event.target instanceof EntityZombie)) {
		//					EntityMob mob = (EntityMob) event.target;
		//					mob.heal(20.0F); // Heal 4 hearts directly
		//					GeneralUtil.enslaveMob((EntityLiving)mob, player);
		//					success = true;
		//				}
		//				if (event.target instanceof EntityZombie) {
		//					EntityZombie zombie = (EntityZombie) event.target;
		//					ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		//					if (zombie.isVillager() && playerEx.getBloodPower() >= (int)(playerEx.getMaxBloodPower()*.5F) ) {
		//						//    	                event.setCanceled(true);
		//						
		//
		//						EntityVillager villager = new EntityVillager(player.worldObj);
		//
		//						// Copy location, rotation, and motion
		//						villager.copyLocationAndAnglesFrom(zombie);
		//						boolean isBaby = zombie.isChild();
		//
		//						villager.onSpawnWithEgg(null);
		//						if (isBaby) {
		//							villager.setGrowingAge(-24000);
		//						}
		//
		//						player.worldObj.removeEntity(zombie);
		//						player.worldObj.spawnEntityInWorld(villager);
		//						ExtendedVillager villagerEx = ExtendedVillager.get(villager);
		//						villagerEx.setBlood(0);
		//
		//						player.worldObj.playAuxSFX(2002, (int)Math.round(villager.posX), (int)Math.round(villager.posY), (int)Math.round(villager.posZ), 0); // Splash potion particles
		//						if (!player.capabilities.isCreativeMode) {
		//							player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Witchery.Items.BLOOD_GOBLET, 1, 0));
		//						}
		//						playerEx.setBloodPower(playerEx.getBloodPower() - (int)(playerEx.getMaxBloodPower()*.5));
		//						success = true;
		//						//    	                    GeneralUtil.sendDebugMessage(player, "The curse is lifted.");
		//
		//					}
		//				}
		//			}
		//
		//			if (event.target instanceof EntityVillager) {
		//				EntityVillager entity = (EntityVillager)event.target;
		//				if (tryConvertToVampire((EntityLiving)entity, ExtendedVillager.get(entity).getBlood(), player, stack))
		//					success = true; 
		//			} else if (event.target instanceof EntityVillageGuard) {
		//				EntityVillageGuard entity = (EntityVillageGuard)event.target;
		//				if (tryConvertToVampire((EntityLiving)entity, entity.getBlood(), player, stack))
		//					success = true; 
		//			} 
		//			if (success) {
		//				ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, world, event.target.posX, event.target.posY, event.target.posZ, event.target.width, event.target.height, 16); // field_70165_t, field_70163_u, field_70161_v, field_70130_N, field_70131_O
		//				setBloodOwner(stack, BloodSource.EMPTY);
		//			} else {
		//				SoundEffect.NOTE_SNARE.playOnlyTo(player);
		//			} 
		//			event.setCanceled(true);
		//		}
		// Cancel on both sides so the client does not run vanilla interact; mutate only on server
		if (hasBlood(stack) && !CreatureUtil.isWerewolf(event.target, true)) {
			event.setCanceled(true);

			if (world.isRemote) {
				return;
			}

			if (getBloodOwner(stack, world) != player) {
				SoundEffect.NOTE_SNARE.playOnlyTo(player);
				return;
			}
			if (!WitcheryUtil.tryConsumeGobletAction(player)) {
				return;
			}

			boolean emptyEntireGoblet = false;

			if (event.target instanceof EntityLivingBase) {
				((EntityLivingBase) event.target).addPotionEffect(new PotionEffect(Potion.regeneration.id, 200, 2));
			}

			if (event.target instanceof EntityVampire) {
				((EntityVampire) event.target).heal(20.0F);
				WitcheryUtil.enslaveMob((EntityLiving)event.target, player);
			} 
			else if (event.target instanceof EntityMob && !(event.target instanceof EntityZombie)) {
				((EntityMob) event.target).heal(20.0F);
				WitcheryUtil.enslaveMob((EntityLiving)event.target, player);
			} 
			else if (event.target instanceof EntityZombie) {
				EntityZombie zombie = (EntityZombie) event.target;
				ExtendedPlayer playerEx = ExtendedPlayer.get(player);

				if (zombie.isVillager() && playerEx.getBloodPower() >= (int)(playerEx.getMaxBloodPower() * 0.5F)) {
					EntityVillager villager = new EntityVillager(world);
					villager.copyLocationAndAnglesFrom(zombie);
					if (zombie.isChild()) villager.setGrowingAge(-24000);
					villager.onSpawnWithEgg(null);

					world.removeEntity(zombie);
					world.spawnEntityInWorld(villager);
					ExtendedVillager.get(villager).setBlood(0);
					world.playAuxSFX(2002, (int)Math.round(villager.posX), (int)Math.round(villager.posY), (int)Math.round(villager.posZ), 0);

					playerEx.setBloodPower(playerEx.getBloodPower() - (int)(playerEx.getMaxBloodPower() * 0.5));
					emptyEntireGoblet = true;
				}
			} 
			else if (event.target instanceof EntityVillager) {
				EntityVillager entity = (EntityVillager)event.target;
				tryConvertToVampire((EntityLiving)entity, ExtendedVillager.get(entity).getBlood(), player, stack);
			} 
			else if (event.target instanceof EntityVillageGuard) {
				EntityVillageGuard entity = (EntityVillageGuard)event.target;
				tryConvertToVampire((EntityLiving)entity, entity.getBlood(), player, stack);
			}

			ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, world, event.target.posX, event.target.posY, event.target.posZ, event.target.width, event.target.height, 16);

			ItemStack used = WitcheryUtil.splitOneGobletForUse(stack);
			if (emptyEntireGoblet) {
				WitcheryUtil.clearGobletBlood(used);
			} else {
				setBloodOwner(used, BloodSource.EMPTY); 
			}
			WitcheryUtil.returnSplitGoblet(player, stack, used);
		}

	}





	@Inject(method = "setBloodOwner(Lnet/minecraft/item/ItemStack;Lcom/emoniph/witchery/item/ItemGlassGoblet$BloodSource;)V", at = @At("HEAD"), cancellable = true)
	public void setBloodOwnerIfEmpty(ItemStack stack, BloodSource source, CallbackInfo ci) {
		if (source == BloodSource.EMPTY) {
			ci.cancel();
			NBTTagCompound nbtRoot = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
			int useCount = nbtRoot.hasKey("BloodUseCounter") ? nbtRoot.getInteger("BloodUseCounter") : 1;
			useCount -= 1;
			if (useCount < 1) {
				WitcheryUtil.clearGobletBlood(stack);
			} else {
				nbtRoot.setInteger("BloodUseCounter", useCount);
				String playerName = nbtRoot.hasKey("WITCPlayerName") ? nbtRoot.getString("WITCPlayerName") : "";
				String uuidS = nbtRoot.hasKey("WITCBloodUUID") ? nbtRoot.getString("WITCBloodUUID") : "";
				if ("__Human".equals(uuidS)) {
					nbtRoot.setString("WITCBloodName", "Human (" + useCount + "/3 Uses)");
				} else {
					nbtRoot.setString("WITCBloodName", playerName + " (" + useCount + "/3 Uses)");
				}
				stack.setItemDamage(1);
				stack.setTagCompound(nbtRoot);
			}
		}
	}

	@Inject(method = "setBloodOwner(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("HEAD"), cancellable = true)
	public void setBloodOwnerAndStoreName(ItemStack stack, EntityPlayer player, CallbackInfo ci) {
		ci.cancel();
		if (player != null) {
			if (!stack.hasTagCompound())
				stack.setTagCompound(new NBTTagCompound());
			NBTTagCompound nbtRoot = stack.getTagCompound();
			nbtRoot.setString("WITCBloodUUID", player.getGameProfile().getId().toString());
			nbtRoot.setString("WITCBloodName", player.getGameProfile().getName() + " (3/3 Uses)");
			nbtRoot.setString("WITCPlayerName", player.getGameProfile().getName());
			nbtRoot.setInteger("BloodUseCounter", 3);
			stack.setItemDamage(1);
		} else {
			WitcheryUtil.clearGobletBlood(stack);
		} 
	}

	@Inject(method = "getBloodOwner", at = @At("HEAD"), cancellable = true)
	private void getBloodOwner(ItemStack stack, World world, CallbackInfoReturnable<EntityPlayer> ci) {

		if (stack.hasTagCompound()) { 

			String s = stack.getTagCompound().getString("WITCBloodUUID");
			if (s == null || s.isEmpty() || s.equals("__Human") || s.equals("__Animal") || BloodSource.isOneOf(s)) {
				ci.setReturnValue(null);
				return;
			}
		}
	}

	//	@Inject(method = "isOneOf", at = @At("HEAD"), cancellable = true )
	//    public static void isOneOfMyBlood(String key, CallbackInfoReturnable<Boolean> ci) {
	//	      if(key.equals("__Human") || key.equals("__Animal")) {
	//	      		ci.setReturnValue(true);
	//	      		ci.cancel();
	//	    	  
	//	      }
	//	    }
}




