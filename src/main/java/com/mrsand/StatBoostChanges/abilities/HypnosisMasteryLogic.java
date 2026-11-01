package com.mrsand.StatBoostChanges.abilities;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.entity.EntityVillageGuard;
import com.emoniph.witchery.entity.EntityWitchHunter;
import com.emoniph.witchery.util.SoundEffect;
import com.emoniph.witchery.util.TimeUtil;
import com.mrsand.StatBoostChanges.abilities.powers.BvVampirePowers;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.HypnosisDebug;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.abilities.LegendsAbilities;
import com.tihyo.legends.effects.LegendsEffects;
import com.tihyo.legends.entities.mobs.EntityPunchingBag;
import com.tihyo.legends.entities.mobs.EntityStand;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.interfaces.IKaiju;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * Hypnosis Mastery: hold to control.
 * <ul>
 *   <li>Stand — stun in place and build a one-session bind (~6s).</li>
 *   <li>Stand again on a bind you own — same ~6s session releases thrall/tame/paralyse.</li>
 *   <li>Sneak — drag toward the block behind them along the look ray (no bind/release).</li>
 * </ul>
 * Purple bar does not drain while channeling.
 */
public final class HypnosisMasteryLogic {

	/** +1 every 5 ticks; threshold 24 → ≈ 6s continuous focus to bind. */
	private static final int ATTEMPT_INTERVAL = 5;
	private static final int PARALYSE_THRESHOLD = 24;
	private static final int TAME_THRESHOLD = 24;
	private static final int THRALL_THRESHOLD = 24;
	private static final int SESSION_GAP_TICKS = 10;
	private static final int THRALL_DURATION_SECS = 240;
	private static final double PULL_SCALE = 0.25D;
	/** Extra vertical authority so lift beats gravity / ground stickiness. */
	private static final double PULL_SCALE_UP = 0.55D;
	private static final double MIN_LIFT = 0.42D;
	private static final double PSYCHIC_RANGE = 25.0D;

	private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<UUID, Session>();

	private HypnosisMasteryLogic() {}

	private static final class Session {
		int targetId = -1;
		int lastChannelTick;
		boolean boundThisSession;
	}

	/**
	 * Call every tick while holding (client + server). Client pass only refreshes the synced
	 * crosshair target; server pass aims / stun-or-pull / bind progress.
	 */
	public static void tickChannel(EntityPlayer player) {
		if (player == null) return;

		EntityLivingBase target = LegendsHelper.getTarget(player, 25.0D);

		if (player.worldObj.isRemote) {
			return;
		}

		int now = LegendsHelper.getTicks(player);
		touchSession(player, now);

		// Infinite channel: bar is never spent here (still gates at > 5 so empty bar blocks use).
		if (VampireRestoredAbilities.getHypnosisStrength(player) <= 5) return;

		if (!player.isSwingInProgress) {
			PowersHandler.aimingRightArm(player, 1, 0);
		}

		boolean touched = target != null && PlayerHelper.canBeTouched(player, target);
		boolean controllable = canControlTarget(target);
		boolean sneaking = player.isSneaking();

		if (HypnosisDebug.enabled() && now % 10 == 0) {
			int key = BvVampirePowers.hypnosisMastery.getAssignedKey(player);
			int duration = key > 0 ? LegacyHelper.getDuration(player, key) : -1;
			Session s = SESSIONS.get(player.getUniqueID());
			Vec3 lookDbg = player.getLook(1.0F);
			HypnosisDebug.logMastery(player,
					"str=" + VampireRestoredAbilities.getHypnosisStrength(player)
							+ " key=" + key + " dur=" + duration
							+ " sneak=" + sneaking
							+ " target=" + (target == null ? "null" : target.getCommandSenderName())
							+ " touched=" + touched + " control=" + controllable
							+ " sessBound=" + (s != null && s.boundThisSession)
							+ " att=" + (target == null ? 0 : target.getEntityData().getInteger("VampireHypnosisAttempts"))
							+ String.format(" look=%.2f,%.2f,%.2f", lookDbg.xCoord, lookDbg.yCoord, lookDbg.zCoord));
		}

		if (target == null || !touched || !controllable) {
			return;
		}

		if (target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.isCreativeMode) {
			return;
		}

		updateSessionTarget(player, target, now);

		if (player.isSwingInProgress) {
			target.addPotionEffect(new PotionEffect(LegendsEffects.hypnosis.id, 200, 0));
			return;
		}

		LegendsAbilities.hypnosis(target);

		if (target instanceof EntityLiving) {
			EntityLiving living = (EntityLiving) target;
			if (living.getNavigator() != null) {
				living.getNavigator().clearPathEntity();
			}
			living.setAttackTarget(null);
			living.setRevengeTarget(null);
		}

		if (target instanceof EntityCreeper) {
			((EntityCreeper) target).setCreeperState(-1);
		}

		if (sneaking) {
			// Psychic control: move target toward the block behind them along the look ray.
			applyPsychicMoveToBlock(player, target);
		} else {
			// Enslavement focus: stun in place (no pull) and build bind.
			applyStun(target);
			tickBindProgress(player, target, now);
		}

		if (HypnosisDebug.enabled() && now % 10 == 0) {
			HypnosisDebug.logMastery(player,
					String.format("%s %s motion=%.3f,%.3f,%.3f",
							sneaking ? "blockPull" : "stun",
							target.getCommandSenderName(), target.motionX, target.motionY, target.motionZ));
		}
	}

	/** Expire abandoned hold sessions so attempt progress does not carry across releases. */
	public static void tickSessionExpiry(EntityPlayer player) {
		if (player == null || player.worldObj.isRemote) return;
		Session s = SESSIONS.get(player.getUniqueID());
		if (s == null) return;
		int now = LegendsHelper.getTicks(player);
		if (now - s.lastChannelTick > SESSION_GAP_TICKS) {
			endSession(player, s, player.worldObj);
		}
	}

	/** Client-only: force a getTarget pass so CURRENT_TARGET_ID syncs while key is held. */
	public static void refreshClientTarget(EntityPlayer player) {
		if (player == null || !player.worldObj.isRemote) return;
		LegendsHelper.getTarget(player, 25.0D);
	}

	/**
	 * Sneak psychic control: look-ray past the entity → next block hit → move entity toward that contact.
	 * No block behind them within range → no yank.
	 */
	private static void applyPsychicMoveToBlock(EntityPlayer player, EntityLivingBase target) {
		Vec3 eye = player.getPosition(1.0F);
		Vec3 look = player.getLook(1.0F);
		double midY = target.posY + (double) target.height * 0.5D;
		double toX = target.posX - eye.xCoord;
		double toY = midY - eye.yCoord;
		double toZ = target.posZ - eye.zCoord;
		double along = toX * look.xCoord + toY * look.yCoord + toZ * look.zCoord;
		if (along < 0.0D) along = 0.0D;

		double past = along + Math.max((double) target.width, (double) target.height) * 0.5D + 0.05D;
		if (past >= PSYCHIC_RANGE) return;

		Vec3 start = eye.addVector(look.xCoord * past, look.yCoord * past, look.zCoord * past);
		Vec3 end = eye.addVector(look.xCoord * PSYCHIC_RANGE, look.yCoord * PSYCHIC_RANGE, look.zCoord * PSYCHIC_RANGE);
		MovingObjectPosition hit = player.worldObj.func_147447_a(start, end, false, false, true);
		if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.hitVec == null) {
			return;
		}

		double dx = hit.hitVec.xCoord - target.posX;
		double dy = hit.hitVec.yCoord - midY;
		double dz = hit.hitVec.zCoord - target.posZ;
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (dist < 1.0E-4D) return;

		target.motionX = dx / dist * PULL_SCALE;
		target.motionZ = dz / dist * PULL_SCALE;

		// Block above the mob (ceiling / higher surface): force real lift, not gravity-cancelled nudge.
		boolean blockAbove = hit.hitVec.yCoord > target.posY + 0.2D;
		if (blockAbove) {
			double lift = dy / dist * PULL_SCALE_UP;
			if (lift < MIN_LIFT) lift = MIN_LIFT;
			target.motionY = lift;
			target.onGround = false;
		} else {
			target.motionY = dy / dist * PULL_SCALE;
		}

		target.fallDistance = 0.0F;
		target.velocityChanged = true;
	}

	/** Hold them still without yanking (stand / enslavement focus). */
	private static void applyStun(EntityLivingBase target) {
		target.motionX = 0.0D;
		target.motionZ = 0.0D;
		if (target.motionY > 0.0D) {
			target.motionY = 0.0D;
		}
		target.fallDistance = 0.0F;
		target.velocityChanged = true;
	}

	private static void tickBindProgress(EntityPlayer player, EntityLivingBase target, int now) {
		if (target instanceof EntityPlayer) return;

		Session s = SESSIONS.get(player.getUniqueID());
		if (s == null || s.boundThisSession) return;
		if (now % ATTEMPT_INTERVAL != 0) return;

		// Stand-hold again on a bind you own → release (~6s new session).
		if (isReleasableBind(player, target)) {
			tickReleaseProgress(player, target, s);
			return;
		}

		// Villager / guard → long paralyse.
		if ((target instanceof EntityVillager || target instanceof EntityVillageGuard)
				&& !(target instanceof com.emoniph.witchery.entity.EntityVillagerWere)) {
			int attempt = bumpAttempts(player, target, PARALYSE_THRESHOLD);
			if (attempt < PARALYSE_THRESHOLD) return;
			if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
				WitcheryUtil.resetHypnosisAttempts(target);
				return;
			}
			WitcheryUtil.resetHypnosisAttempts(target);
			int amp = WitcheryUtil.getVampireLevel(player) >= 8 ? 5 : 4;
			target.addPotionEffect(new PotionEffect(Witchery.Potions.PARALYSED.id,
					TimeUtil.secsToTicks(THRALL_DURATION_SECS), amp));
			if (target instanceof EntityVillageGuard) {
				EntityVillageGuard guard = (EntityVillageGuard) target;
				guard.setTarget(null);
				guard.setAttackTarget(null);
				guard.setRevengeTarget(null);
			}
			if (target instanceof EntityVillager) {
				GeneralUtil.removeAvoidanceTasks((EntityVillager) target);
			}
			notifyBind(player, "Their mind locks — they are under your sway");
			playBindSuccess(player, target, true);
			s.boundThisSession = true;
			return;
		}

		// Tameables → tame once if untamed.
		if (target instanceof EntityTameable) {
			EntityTameable tameable = (EntityTameable) target;
			if (tameable.isTamed()) {
				// Someone else's pet — do not steal or release.
				s.boundThisSession = true;
				return;
			}
			int attempt = bumpAttempts(player, target, TAME_THRESHOLD);
			if (attempt < TAME_THRESHOLD) return;
			if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
				WitcheryUtil.resetHypnosisAttempts(target);
				return;
			}
			WitcheryUtil.resetHypnosisAttempts(target);
			tameable.setTamed(true);
			tameable.func_152115_b(player.getUniqueID().toString());
			if (tameable instanceof net.minecraft.entity.passive.EntityOcelot) {
				((net.minecraft.entity.passive.EntityOcelot) tameable)
						.setTameSkin(1 + tameable.worldObj.rand.nextInt(3));
			}
			tameable.worldObj.setEntityState(tameable, (byte) 7);
			tameable.setAttackTarget(null);
			tameable.setRevengeTarget(null);
			if (tameable.getNavigator() != null) {
				tameable.getNavigator().clearPathEntity();
			}
			notifyBind(player, "It's now loyal to you");
			playBindSuccess(player, target, true);
			s.boundThisSession = true;
			return;
		}

		// Other creatures → DIY thrall once.
		if (target instanceof EntityCreature
				&& !(target instanceof EntityWitchHunter)
				&& !(target instanceof EntitySquid)) {
			EntityLiving living = (EntityLiving) target;
			if (WitcheryUtil.isThralled(living)) {
				// Another controller's thrall — do not steal.
				s.boundThisSession = true;
				return;
			}
			int attempt = bumpAttempts(player, target, THRALL_THRESHOLD);
			if (attempt < THRALL_THRESHOLD) return;
			if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
				WitcheryUtil.resetHypnosisAttempts(target);
				return;
			}
			WitcheryUtil.resetHypnosisAttempts(living);
			living.getEntityData().removeTag("fearTimer");
			WitcheryUtil.startThrall(living, player, TimeUtil.secsToTicks(THRALL_DURATION_SECS));
			// startThrall already plays the hypnosis sound — avoid doubling it.
			notifyBind(player, "It bends to your will");
			playBindSuccess(player, target, false);
			s.boundThisSession = true;
		}
	}

	/** True when stand-hold should release instead of bind. */
	private static boolean isReleasableBind(EntityPlayer player, EntityLivingBase target) {
		if ((target instanceof EntityVillager || target instanceof EntityVillageGuard)
				&& !(target instanceof com.emoniph.witchery.entity.EntityVillagerWere)
				&& target.isPotionActive(Witchery.Potions.PARALYSED)) {
			return true;
		}
		if (target instanceof EntityTameable) {
			EntityTameable tameable = (EntityTameable) target;
			return tameable.isTamed() && tameable.func_152114_e(player);
		}
		if (target instanceof EntityLiving) {
			return WitcheryUtil.isThralledBy((EntityLiving) target, player);
		}
		return false;
	}

	private static void tickReleaseProgress(EntityPlayer player, EntityLivingBase target, Session s) {
		int attempt = bumpAttempts(player, target, THRALL_THRESHOLD);
		if (attempt < THRALL_THRESHOLD) return;
		if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
			WitcheryUtil.resetHypnosisAttempts(target);
			return;
		}
		WitcheryUtil.resetHypnosisAttempts(target);

		if ((target instanceof EntityVillager || target instanceof EntityVillageGuard)
				&& target.isPotionActive(Witchery.Potions.PARALYSED)) {
			target.removePotionEffect(Witchery.Potions.PARALYSED.id);
		} else if (target instanceof EntityTameable) {
			EntityTameable tameable = (EntityTameable) target;
			tameable.setTamed(false);
			tameable.func_152115_b("");
			tameable.setAttackTarget(null);
			tameable.setRevengeTarget(null);
			if (tameable.getNavigator() != null) {
				tameable.getNavigator().clearPathEntity();
			}
			tameable.worldObj.setEntityState(tameable, (byte) 6);
		} else if (target instanceof EntityLiving) {
			WitcheryUtil.clearThrall((EntityLiving) target);
		}

		notifyBind(player, "You release your hold on their mind");
		playBindSuccess(player, target, true);
		s.boundThisSession = true;
	}

	private static int bumpAttempts(EntityPlayer player, EntityLivingBase target, int threshold) {
		NBTTagCompound nbt = target.getEntityData();
		int attempt = nbt.getInteger("VampireHypnosisAttempts") + 1;
		nbt.setInteger("VampireHypnosisAttempts", attempt);
		playBindProgress(player, attempt, threshold);
		return attempt;
	}

	/** Ding at start; rising hypnosis pitch while focus builds — no chat until success. */
	private static void playBindProgress(EntityPlayer player, int attempt, int threshold) {
		if (attempt == 1) {
			player.worldObj.playSoundAtEntity(player, "random.orb", 1.0F, 5.0F);
			return;
		}
		float progress = (float) attempt / (float) threshold;
		float pitch = 0.1F + (progress * 1.9F);
		WitcheryUtil.playHypnoSound(player.worldObj, player, 0.35F, pitch);
	}

	private static void playBindSuccess(EntityPlayer player, EntityLivingBase target, boolean playHypnosisSound) {
		if (playHypnosisSound) {
			SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playAtPlayer(target.worldObj, player, 0.5F, 1.0F);
		}
		player.worldObj.playSoundAtEntity(player, "mob.wolf.growl", 1.0F, 1.0F);
	}

	private static void notifyBind(EntityPlayer player, String message) {
		if (player == null || player.worldObj.isRemote) return;
		player.addChatMessage(new ChatComponentText(message)
				.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
	}

	private static void touchSession(EntityPlayer player, int now) {
		Session s = SESSIONS.get(player.getUniqueID());
		if (s != null && now - s.lastChannelTick > SESSION_GAP_TICKS) {
			endSession(player, s, player.worldObj);
			s = null;
		}
		if (s == null) {
			s = new Session();
			SESSIONS.put(player.getUniqueID(), s);
		}
		s.lastChannelTick = now;
	}

	private static void updateSessionTarget(EntityPlayer player, EntityLivingBase target, int now) {
		Session s = SESSIONS.get(player.getUniqueID());
		if (s == null) {
			touchSession(player, now);
			s = SESSIONS.get(player.getUniqueID());
		}
		int id = target.getEntityId();
		if (s.targetId != id) {
			if (s.targetId > 0) {
				Entity old = player.worldObj.getEntityByID(s.targetId);
				if (old instanceof EntityLivingBase) {
					WitcheryUtil.resetHypnosisAttempts((EntityLivingBase) old);
				}
			}
			s.targetId = id;
			s.boundThisSession = false;
			WitcheryUtil.resetHypnosisAttempts(target);
		}
		s.lastChannelTick = now;
	}

	private static void endSession(EntityPlayer player, Session s, net.minecraft.world.World world) {
		if (s == null) return;
		if (s.targetId > 0 && world != null) {
			Entity old = world.getEntityByID(s.targetId);
			if (old instanceof EntityLivingBase) {
				WitcheryUtil.resetHypnosisAttempts((EntityLivingBase) old);
			}
		}
		SESSIONS.remove(player.getUniqueID());
	}

	/** Inlined Legends 5.4 {@code LegendsHelper.canLiftTarget} eligibility. */
	private static boolean canControlTarget(EntityLivingBase target) {
		return target != null
				&& !(target instanceof IKaiju)
				&& !(target instanceof EntityPunchingBag)
				&& !(target instanceof EntityStand)
				&& target.height < 20.0F;
	}
}
