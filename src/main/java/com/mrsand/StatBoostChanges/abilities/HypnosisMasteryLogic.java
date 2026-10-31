package com.mrsand.StatBoostChanges.abilities;

import com.mrsand.StatBoostChanges.abilities.powers.BvVampirePowers;
import com.mrsand.StatBoostChanges.util.HypnosisDebug;
import com.tihyo.legends.abilities.LegendsAbilities;
import com.tihyo.legends.effects.LegendsEffects;
import com.tihyo.legends.entities.mobs.EntityPunchingBag;
import com.tihyo.legends.entities.mobs.EntityStand;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.interfaces.IKaiju;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.Vec3;

/**
 * Legends 5.4 Hypnosis Mastery control.
 * <p>
 * Targeting matches Legends 5 {@code LegendsHelper.getTarget}: the <b>client</b> raycasts and
 * syncs {@code CURRENT_TARGET_ID} to the server; the <b>server</b> reads that ID and applies pull.
 */
public final class HypnosisMasteryLogic {

	private HypnosisMasteryLogic() {}

	/**
	 * Call every tick while holding (client + server). Client pass only refreshes the synced
	 * crosshair target; server pass drains / aims / pulls.
	 */
	public static void tickChannel(EntityPlayer player) {
		if (player == null) return;

		// Legends 5: client raycast → CURRENT_TARGET_ID packet → server lookup.
		EntityLivingBase target = LegendsHelper.getTarget(player, 25.0D);

		if (player.worldObj.isRemote) {
			return;
		}

		if (VampireRestoredAbilities.getHypnosisStrength(player) <= 5) return;

		if (!player.isSwingInProgress) {
			PowersHandler.aimingRightArm(player, 1, 0);
		}

		// Legends 5.4 dual drain: common handler (−1/2 ticks) + execute (−1/10 ticks).
		if (LegendsHelper.getTicks(player) % 2 == 0) {
			VampireRestoredAbilities.addHypnosisStrength(player, -1);
		}
		if (LegendsHelper.getTicks(player) % 10 == 0) {
			VampireRestoredAbilities.addHypnosisStrength(player, -1);
		}

		boolean touched = target != null && PlayerHelper.canBeTouched(player, target);
		boolean controllable = canControlTarget(target);

		if (HypnosisDebug.enabled() && LegendsHelper.getTicks(player) % 10 == 0) {
			int key = BvVampirePowers.hypnosisMastery.getAssignedKey(player);
			int duration = key > 0 ? LegacyHelper.getDuration(player, key) : -1;
			Vec3 lookDbg = player.getLook(1.0F);
			HypnosisDebug.logMastery(player,
					"str=" + VampireRestoredAbilities.getHypnosisStrength(player)
							+ " key=" + key + " dur=" + duration
							+ " target=" + (target == null ? "null" : target.getCommandSenderName())
							+ " touched=" + touched + " control=" + controllable
							+ String.format(" look=%.2f,%.2f,%.2f", lookDbg.xCoord, lookDbg.yCoord, lookDbg.zCoord));
		}

		if (target == null || !touched || !controllable) {
			return;
		}

		if (target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.isCreativeMode) {
			return;
		}

		if (player.isSwingInProgress) {
			target.addPotionEffect(new PotionEffect(LegendsEffects.hypnosis.id, 200, 0));
			VampireRestoredAbilities.addHypnosisStrength(player, -10);
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

		// Aim offsets match Legends 5 hypnosisControl (*3/*5/*3). Scale 0.25 is snappier
		// than stock 0.1 (still below force-choke 0.5). Y only when target is below caster.
		final double pullScale = 0.25D;
		Vec3 look = player.getLook(1.0F);
		double dx = player.posX + look.xCoord * 3.0D - target.posX;
		double dy = player.posY + look.yCoord * 5.0D - target.posY;
		double dz = player.posZ + look.zCoord * 3.0D - target.posZ;
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (dist < 1.0E-4D) return;

		double mx = dx / dist * pullScale;
		double mz = dz / dist * pullScale;

		target.motionX = mx;
		if (target.posY < player.posY) {
			target.motionY = dy / dist * pullScale;
		}
		target.motionZ = mz;
		target.fallDistance = 0.0F;
		target.velocityChanged = true;

		if (HypnosisDebug.enabled() && LegendsHelper.getTicks(player) % 10 == 0) {
			HypnosisDebug.logMastery(player,
					String.format("pull %s motion=%.3f,%.3f,%.3f dist=%.2f",
							target.getCommandSenderName(), target.motionX, target.motionY, target.motionZ, dist));
		}
	}

	/** Client-only: force a getTarget pass so CURRENT_TARGET_ID syncs while key is held. */
	public static void refreshClientTarget(EntityPlayer player) {
		if (player == null || !player.worldObj.isRemote) return;
		LegendsHelper.getTarget(player, 25.0D);
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
