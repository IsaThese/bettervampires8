package com.mrsand.StatBoostChanges.abilities;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.entity.EntityVillageGuard;
import com.emoniph.witchery.entity.EntityWitchHunter;
import com.emoniph.witchery.network.PacketParticles;
import com.emoniph.witchery.util.ParticleEffect;
import com.emoniph.witchery.util.SoundEffect;
import com.emoniph.witchery.util.TargetPointUtil;
import com.emoniph.witchery.util.TimeUtil;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.management.LegendsHelper;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;

/** Witchery channel/thrall behavior for BetterVampires Hypnosis Mastery. */
public final class HypnosisMasteryLogic {

	private HypnosisMasteryLogic() {}

	public static void tickChannel(EntityPlayer player) {
		if (player == null || VampireRestoredAbilities.getHypnosisStrength(player) <= 5
				|| GeneralUtil.isInLight(player)) {
			return;
		}

		EntityLivingBase target = LegendsHelper.getTarget(player, 25.0D);
		if (target == null) {
			return;
		}

		ExtendedPlayer playerEx = ExtendedPlayer.get(player);

		WitcheryUtil.beginHypnosisChannel(player);
		GeneralUtil.forceLookAtEntity(target, player);
		WitcheryUtil.applyChannelFloat(target);

		if (LegendsHelper.getTicks(player) % 20 == 0) {
			VampireRestoredAbilities.addHypnosisStrength(player, -1);
		}

		if (!player.isSwingInProgress) {
			PowersHandler.aimingRightArm(player, 1, 0);
		}

		if (player.isSwingInProgress) {
			VampireRestoredAbilities.addHypnosisStrength(player, -10);
		}

		if (target instanceof EntityCreeper) {
			((EntityCreeper) target).setCreeperState(-1);
		}

		if ((target instanceof EntityVillager || target instanceof EntityVillageGuard)
				&& !(target instanceof com.emoniph.witchery.entity.EntityVillagerWere)) {
			NBTTagCompound nbt = target.getEntityData();
			int attempt = nbt.getInteger("VampireHypnosisAttempts") + 1;
			nbt.setInteger("VampireHypnosisAttempts", attempt);

			if (attempt > 10 && !target.isPotionActive(Witchery.Potions.PARALYSED)) {
				if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
					WitcheryUtil.resetHypnosisAttempts(target);
					WitcheryUtil.endHypnosisChannel(player);
					return;
				}
				WitcheryUtil.resetHypnosisAttempts(target);
				if (!player.worldObj.isRemote) {
					target.addPotionEffect(new PotionEffect(Witchery.Potions.PARALYSED.id, TimeUtil.secsToTicks(240),
							(playerEx.getVampireLevel() >= 8) ? 5 : 4));
				}
				SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playAtPlayer(target.worldObj, player, 0.5F, 1.0F);
				if (target instanceof EntityVillageGuard) {
					EntityVillageGuard guard = (EntityVillageGuard) target;
					guard.setTarget(null);
					guard.setAttackTarget(null);
					guard.setRevengeTarget(null);
				}
				if (target instanceof EntityVillager) {
					GeneralUtil.removeAvoidanceTasks((EntityVillager) target);
				}
				WitcheryUtil.endHypnosisChannel(player);
			}
			return;
		}

		if (target instanceof net.minecraft.entity.passive.EntityTameable) {
			net.minecraft.entity.passive.EntityTameable tameable = (net.minecraft.entity.passive.EntityTameable) target;
			NBTTagCompound nbt = target.getEntityData();
			if (player.ticksExisted % 5 == 0) {
				nbt.setInteger("VampireHypnosisAttempts", nbt.getInteger("VampireHypnosisAttempts") + 1);
			}
			int attempt = nbt.getInteger("VampireHypnosisAttempts");

			if (attempt >= 12) {
				if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
					WitcheryUtil.resetHypnosisAttempts(target);
					WitcheryUtil.endHypnosisChannel(player);
					return;
				}
				WitcheryUtil.resetHypnosisAttempts(target);

				net.minecraft.util.ChatStyle style = new net.minecraft.util.ChatStyle()
						.setBold(true)
						.setColor(net.minecraft.util.EnumChatFormatting.RED);

				String feedback;
				if (!tameable.isTamed()) {
					tameable.setTamed(true);
					tameable.func_152115_b(player.getUniqueID().toString());
					if (tameable instanceof net.minecraft.entity.passive.EntityOcelot) {
						((net.minecraft.entity.passive.EntityOcelot) tameable).setTameSkin(1 + tameable.worldObj.rand.nextInt(3));
					}
					tameable.worldObj.setEntityState(tameable, (byte) 7);
					feedback = "It's now loyal to you";
				} else {
					tameable.setTamed(false);
					tameable.func_152115_b("");
					if (tameable instanceof net.minecraft.entity.passive.EntityOcelot) {
						((net.minecraft.entity.passive.EntityOcelot) tameable).setTameSkin(0);
					}
					tameable.worldObj.setEntityState(tameable, (byte) 6);
					feedback = "It forgets its owner";
				}

				tameable.setAttackTarget(null);
				tameable.setRevengeTarget(null);
				tameable.getNavigator().clearPathEntity();

				if (!player.worldObj.isRemote) {
					player.addChatMessage(new ChatComponentText(feedback).setChatStyle(style));
				}

				SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playAtPlayer(target.worldObj, player, 0.5F, 1.0F);
				WitcheryUtil.endHypnosisChannel(player);
			}
			return;
		}

		if (target instanceof EntityCreature && !(target instanceof EntityWitchHunter) && !(target instanceof EntitySquid)) {
			EntityLiving targetLiving = (EntityLiving) target;
			NBTTagCompound nbt = target.getEntityData();
			if (player.ticksExisted % 5 == 0) {
				nbt.setInteger("VampireHypnosisAttempts", nbt.getInteger("VampireHypnosisAttempts") + 1);
			}

			int attempt = nbt.getInteger("VampireHypnosisAttempts");

			if (!WitcheryUtil.isThralledBy(targetLiving, player) && attempt >= 12) {
				if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
					WitcheryUtil.resetHypnosisAttempts(targetLiving);
					WitcheryUtil.endHypnosisChannel(player);
					return;
				}
				WitcheryUtil.resetHypnosisAttempts(targetLiving);
				nbt.removeTag("fearTimer");

				WitcheryUtil.startThrall(targetLiving, player, TimeUtil.secsToTicks(240));
				SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playAtPlayer(target.worldObj, player, 0.5F, 1.0F);
				if (!player.worldObj.isRemote) {
					player.addChatMessage(new ChatComponentText("It bends to your will"));
				}
				WitcheryUtil.endHypnosisChannel(player);

			} else if (WitcheryUtil.isThralledBy(targetLiving, player) && attempt >= 20) {
				WitcheryUtil.resetHypnosisAttempts(targetLiving);
				WitcheryUtil.clearThrall(targetLiving);
				Witchery.packetPipeline.sendToAllAround(
						(IMessage) new PacketParticles(ParticleEffect.MOB_SPELL, SoundEffect.MOB_ZOMBIE_INFECT, targetLiving, 0.5D, 1.0D),
						TargetPointUtil.from(targetLiving, 16.0D));
				if (!player.worldObj.isRemote) {
					player.addChatMessage(new ChatComponentText("You relinquish control"));
				}
				if (target instanceof EntityMob) {
					EntityMob mob = (EntityMob) target;
					if (mob instanceof EntitySkeleton) {
						GeneralUtil.restoreSkeletonTask((EntitySkeleton) mob);
					} else if (mob instanceof EntityZombie) {
						GeneralUtil.restoreZombieTask((EntityZombie) mob);
						GeneralUtil.sendDebugMessage(player, "You restore the undead's desires");
					} else {
						GeneralUtil.restoreGeneralTargetTasks(mob);
					}
				}
				WitcheryUtil.endHypnosisChannel(player);
			}
		}
	}
}
