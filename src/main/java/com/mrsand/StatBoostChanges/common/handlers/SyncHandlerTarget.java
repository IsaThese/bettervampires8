package com.mrsand.StatBoostChanges.common.handlers;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.util.TimeUtil;
import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.mrsand.StatBoostChanges.common.packets.PacketSyncTarget;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.HypnosisDebug;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;

public class SyncHandlerTarget implements IMessageHandler<PacketSyncTarget, IMessage> {
	// client to server
	@Override
	public IMessage onMessage(PacketSyncTarget message, MessageContext ctx) {
		EntityPlayer player = ctx.getServerHandler().playerEntity;
		Entity entity = player.worldObj.getEntityByID(message.getEntityID());
		if (entity instanceof EntityCreature && player != null) {
			// Belt-and-suspenders: Mastery channel must not run old gaze bind/ding.
			if (VampireRestoredAbilities.isHypnosisMasteryChanneling(player)) {
				if (HypnosisDebug.dragEnabled()) {
					HypnosisDebug.logBind(player, "gazePacket ignored — mastery channeling id="
							+ message.getEntityID() + " sneak=" + player.isSneaking());
				}
				return null;
			}
			EntityCreature target = (EntityCreature) entity;

			GeneralUtil.forceLookAtEntity(target, player);
			WitcheryUtil.applyChannelFloat(target);

			if (target instanceof EntityCreeper) {
				((EntityCreeper) target).setCreeperState(-1);
			}

			int tick = player.ticksExisted;
			NBTTagCompound nbt = target.getEntityData();
			int attempt = nbt.getInteger("VampireHypnosisAttempts");

			if (nbt.getBoolean("tranced") && !nbt.getBoolean("angry") && tick % 20 == 0) {
				int currentTimer = nbt.getInteger("trancedTimer");
				float max = 100F;
				int newTimer = (int) Math.min(currentTimer + 8, max);
				if (newTimer > currentTimer) {
					nbt.setInteger("trancedTimer", newTimer);
				}
				nbt.setString("vampirePlayerUUID", player.getUniqueID().toString());
				if (player.ticksExisted % 20 == 0) {
					float progress = (float) newTimer / max;
					float pitch = 0.1F + (progress * 1.9F);
					if (currentTimer < (int) (max - 10) && newTimer == (int) max) {
						player.worldObj.playSoundAtEntity(player, "mob.wolf.growl", 1.0F, 1.0F);
					} else if (newTimer < (int) max) {
						WitcheryUtil.playHypnoSound(player.worldObj, player, .3F, pitch);
					}
				}
				return null;
			}

			if (tick % 7 == 0 && (!nbt.getBoolean("tranced") || nbt.getBoolean("angry"))) {
				if (attempt < 15) {
					if (attempt == 0) {
						if (HypnosisDebug.dragEnabled()) {
							HypnosisDebug.logBind(player,
									"gazeDing random.orb id=" + target.getEntityId()
											+ " name=" + target.getCommandSenderName()
											+ " sneak=" + player.isSneaking());
						}
						player.worldObj.playSoundAtEntity(player, "random.orb", 1.0F, 5.0F);
					}
					attempt++;
					nbt.setInteger("VampireHypnosisAttempts", attempt);
				} else {
					if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
						WitcheryUtil.resetHypnosisAttempts(target);
						WitcheryUtil.endHypnosisChannel(player);
						return null;
					}
					WitcheryUtil.resetHypnosisAttempts(target);
					nbt.removeTag("angry");
					if (target instanceof EntityVillager) {
						target.addPotionEffect(new PotionEffect(Witchery.Potions.PARALYSED.id, TimeUtil.secsToTicks(240),
								(WitcheryUtil.getVampireLevel(player) >= 8) ? 5 : 4));
					} else {
						WitcheryUtil.startThrall((EntityLiving) target, player, TimeUtil.secsToTicks(240));
					}
					player.worldObj.playSoundAtEntity(player, "mob.wolf.growl", 1.0F, 1.0F);
					WitcheryUtil.endHypnosisChannel(player);
				}
			}
		}

		return null;
	}
}
