package com.mrsand.StatBoostChanges.util;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;
import com.tihyo.legends.management.properties.LegendsProperties;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Shared sticky latch targeting for Bite, Drain, Wrath, and Vampirization:
 * acquire at 3–5 blocks, snap victim into stock ~2-block anim range, optional
 * once-per-latch Witchery blood grant.
 */
public final class VampireLatchHits {

	public enum Channel {
		BITE("bvBiteLatched", "bvBiteTargetId"),
		DRAIN("bvDrainLatched", null),
		WRATH("bvWrathLatched", "bvWrathTargetId"),
		VAMPIRIZATION("bvVampzLatched", "bvVampzTargetId");

		public final String latchedKey;
		/** Null = use Legends CURRENT_TARGET_ID (Drain). */
		public final String targetIdKey;

		Channel(String latchedKey, String targetIdKey) {
			this.latchedKey = latchedKey;
			this.targetIdKey = targetIdKey;
		}
	}

	/** Stock Horror bite / convert reach used for animation / post-snap. */
	public static final double ANIM_RANGE = 2.0D;

	private VampireLatchHits() {}

	public static EntityLivingBase getBiteTarget(EntityPlayer player) {
		return getStickyLatchTarget(
				player,
				Channel.BITE,
				StatBoostConfig.vampiricBiteRange,
				StatBoostConfig.vampiricBiteWideness,
				true,
				StatBoostConfig.biteLatchBloodFraction,
				true);
	}

	/** Lord Wrath: same latch as bite; Witchery 1/3 once. */
	public static EntityLivingBase getWrathTarget(EntityPlayer player) {
		return getStickyLatchTarget(
				player,
				Channel.WRATH,
				StatBoostConfig.vampiricBiteRange,
				StatBoostConfig.vampiricBiteWideness,
				true,
				StatBoostConfig.wrathLatchBloodFraction,
				false);
	}

	/** Vampirization: latch + snap only (no Witchery grant). */
	public static EntityLivingBase getVampirizationTarget(EntityPlayer player) {
		return getStickyLatchTarget(
				player,
				Channel.VAMPIRIZATION,
				StatBoostConfig.vampiricBiteRange,
				StatBoostConfig.vampiricBiteWideness,
				true,
				0.0F,
				false);
	}

	private static EntityLivingBase getStickyLatchTarget(
			EntityPlayer player,
			Channel channel,
			double range,
			float widen,
			boolean snap,
			float bloodFraction,
			boolean applyTrueFormMult) {
		if (player == null || player.worldObj == null) return null;
		boolean remote = player.worldObj.isRemote;

		if (remote) {
			EntityLivingBase cross = VampiricDrainHits.findWideCrosshair(player, range, widen);
			if (cross != null) {
				setTargetId(player, channel, cross.getEntityId());
			}
		}

		EntityLivingBase sticky = getStickyTarget(player, channel, range);
		if (sticky != null) {
			if (!remote) {
				tryLatch(player, channel, sticky, snap, bloodFraction, applyTrueFormMult);
			}
			return sticky;
		}
		if (!remote) {
			EntityLivingBase found = VampiricDrainHits.findWideCrosshair(player, range, widen);
			if (found != null) {
				setTargetId(player, channel, found.getEntityId());
				tryLatch(player, channel, found, snap, bloodFraction, applyTrueFormMult);
			}
			return found;
		}
		return null;
	}

	private static EntityLivingBase getStickyTarget(EntityPlayer player, Channel channel, double range) {
		int id = getTargetId(player, channel);
		if (id <= 0) return null;
		Entity e = player.worldObj.getEntityByID(id);
		if (!(e instanceof EntityLivingBase)) return null;
		double centerDist = player.getDistanceToEntity(e);
		double aabbDist = VampiricDrainHits.distanceToAabb(player, e);
		if (aabbDist > range && centerDist > range) {
			return null;
		}
		return (EntityLivingBase) e;
	}

	/**
	 * First successful lock this cast: optional snap + Witchery grant.
	 * Idempotent while the channel latch flag is set.
	 */
	public static void tryLatch(
			EntityPlayer player,
			Channel channel,
			EntityLivingBase target,
			boolean snap,
			float bloodFraction,
			boolean applyTrueFormMult) {
		if (player == null || target == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (player.getEntityData().getBoolean(channel.latchedKey)) return;
		if (!isChannelActive(player, channel)) return;

		player.getEntityData().setBoolean(channel.latchedKey, true);
		setTargetId(player, channel, target.getEntityId());

		if (snap) {
			snapVictimToAnimRange(player, target);
		}
		if (bloodFraction > 0.0F) {
			float fraction = bloodFraction;
			if (applyTrueFormMult && LegendsUtil.isDraculaTrueForm(player)) {
				fraction *= StatBoostConfig.trueFormBloodGainMult;
			}
			grantLatchWitcheryBlood(player, fraction);
		}
	}

	/** Drain latch: Witchery once; no snap (long-range channel). */
	public static void tryDrainLatch(EntityPlayer player, EntityLivingBase target) {
		if (player == null || target == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (player.getEntityData().getBoolean(Channel.DRAIN.latchedKey)) return;
		if (LegendsAbilityBridge.getIntProp(player, LegendsProperties.BLOOD_DRAIN) <= 0) return;
		player.getEntityData().setBoolean(Channel.DRAIN.latchedKey, true);
		grantLatchWitcheryBlood(player, StatBoostConfig.drainLatchBloodFraction);
	}

	public static void clearEndedChannels(EntityPlayer player) {
		if (player == null) return;
		clearIfEnded(player, Channel.BITE, LegendsProperties.VAMPIRE_BITE);
		clearIfEnded(player, Channel.DRAIN, LegendsProperties.BLOOD_DRAIN);
		clearIfEnded(player, Channel.WRATH, LegendsProperties.VAMPIRES_WRATH);
		clearIfEnded(player, Channel.VAMPIRIZATION, LegendsProperties.VAMPIRIZATION);
	}

	private static void clearIfEnded(EntityPlayer player, Channel channel, LegendsProperties prop) {
		if (LegendsAbilityBridge.getIntProp(player, prop) > 0) return;
		player.getEntityData().removeTag(channel.latchedKey);
		if (channel.targetIdKey != null) {
			player.getEntityData().removeTag(channel.targetIdKey);
		}
	}

	private static boolean isChannelActive(EntityPlayer player, Channel channel) {
		switch (channel) {
			case BITE:
				return LegendsAbilityBridge.getIntProp(player, LegendsProperties.VAMPIRE_BITE) > 0;
			case DRAIN:
				return LegendsAbilityBridge.getIntProp(player, LegendsProperties.BLOOD_DRAIN) > 0;
			case WRATH:
				return LegendsAbilityBridge.getIntProp(player, LegendsProperties.VAMPIRES_WRATH) > 0;
			case VAMPIRIZATION:
				return LegendsAbilityBridge.getIntProp(player, LegendsProperties.VAMPIRIZATION) > 0;
			default:
				return false;
		}
	}

	/** Zero stock meter pulses while biting (Witchery lump replaces them). */
	public static int suppressBiteMeterPulse(EntityPlayer player, int amount) {
		if (player == null) return amount;
		if (LegendsAbilityBridge.getIntProp(player, LegendsProperties.VAMPIRE_BITE) > 0) {
			return 0;
		}
		return amount;
	}

	/**
	 * Lords on Wrath: Witchery latch replaces stock meter pulses + BV ≥30 floor.
	 */
	public static int suppressWrathMeterPulse(EntityPlayer player, int amount) {
		if (player == null) return amount;
		if (!DaywalkerControl.isVampireLord(player)) return amount;
		if (LegendsAbilityBridge.getIntProp(player, LegendsProperties.VAMPIRES_WRATH) > 0) {
			return 0;
		}
		return amount;
	}

	private static int getTargetId(EntityPlayer player, Channel channel) {
		if (channel.targetIdKey == null) {
			try {
				Object raw = LegendsProperties.CURRENT_TARGET_ID.get(player);
				return raw instanceof Integer ? ((Integer) raw).intValue() : 0;
			} catch (Throwable t) {
				return 0;
			}
		}
		return player.getEntityData().getInteger(channel.targetIdKey);
	}

	private static void setTargetId(EntityPlayer player, Channel channel, int id) {
		if (channel.targetIdKey == null) {
			try {
				LegendsProperties.CURRENT_TARGET_ID.set(player, Integer.valueOf(id));
			} catch (Throwable ignored) {
			}
			return;
		}
		player.getEntityData().setInteger(channel.targetIdKey, id);
	}

	public static void snapVictimToAnimRange(EntityPlayer player, EntityLivingBase victim) {
		double dx = victim.posX - player.posX;
		double dy = victim.posY - player.posY;
		double dz = victim.posZ - player.posZ;
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (dist <= ANIM_RANGE || dist < 1.0E-3D) return;

		double horiz = Math.sqrt(dx * dx + dz * dz);
		double nx;
		double ny = victim.posY;
		double nz;
		if (horiz > 1.0E-3D) {
			double hScale = ANIM_RANGE / horiz;
			if (Math.abs(dy) > ANIM_RANGE) {
				double s = ANIM_RANGE / dist;
				nx = player.posX + dx * s;
				ny = player.posY + dy * s;
				nz = player.posZ + dz * s;
			} else {
				nx = player.posX + dx * hScale;
				nz = player.posZ + dz * hScale;
				double after = Math.sqrt(
						(nx - player.posX) * (nx - player.posX)
								+ (ny - player.posY) * (ny - player.posY)
								+ (nz - player.posZ) * (nz - player.posZ));
				if (after > ANIM_RANGE + 0.25D) {
					double s = ANIM_RANGE / dist;
					nx = player.posX + dx * s;
					ny = player.posY + dy * s;
					nz = player.posZ + dz * s;
				}
			}
		} else {
			double s = ANIM_RANGE / dist;
			nx = player.posX + dx * s;
			ny = player.posY + dy * s;
			nz = player.posZ + dz * s;
		}
		teleportVictim(victim, nx, ny, nz);
	}

	private static void teleportVictim(EntityLivingBase victim, double x, double y, double z) {
		victim.fallDistance = 0.0F;
		victim.motionX = 0.0D;
		victim.motionY = 0.0D;
		victim.motionZ = 0.0D;
		if (victim instanceof EntityPlayerMP) {
			EntityPlayerMP mp = (EntityPlayerMP) victim;
			mp.playerNetServerHandler.setPlayerLocation(x, y, z, mp.rotationYaw, mp.rotationPitch);
		} else {
			victim.setPositionAndUpdate(x, y, z);
		}
	}

	public static void grantLatchWitcheryBlood(EntityPlayer player, float fraction) {
		if (!Loader.isModLoaded("witchery") || player == null) return;
		if (fraction <= 0.0F) return;
		try {
			int max = WitcheryUtil.getMaxBloodPower(player);
			int amount = (int) Math.ceil(max * (double) fraction);
			if (amount > 0) {
				WitcheryUtil.increaseBloodPower(player, amount);
			}
		} catch (Throwable ignored) {
		}
	}
}
