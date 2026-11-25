package com.mrsand.StatBoostChanges.mixins.early;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.GeneralUtil;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Bat roosting for vampires: hang under any solid ceiling (leaves, castle
 * blocks, etc.), wake-ignore vampires within 4 blocks, and seek hang spots
 * more often when a vampire is nearby.
 * <p>
 * Early-mixin style matches {@link EntitySpiderMixin}/{@link EntityGhastMixin}:
 * extend the vanilla superclass for protected members; cast for public bat
 * API; redirects + inject only — no MCP {@code @Shadow} methods/fields.
 */
@Mixin(EntityBat.class)
public abstract class EntityBatMixin extends EntityAmbientCreature {

	private static final double CALM_RANGE = 16.0D;

	public EntityBatMixin(World world) {
		super(world);
	}

	@Redirect(
			method = "updateAITasks",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/World;getClosestPlayerToEntity(Lnet/minecraft/entity/Entity;D)Lnet/minecraft/entity/player/EntityPlayer;"
			)
	)
	private EntityPlayer ignoreVampireWake(World world, Entity bat, double distance) {
		EntityPlayer closest = world.getClosestPlayerToEntity(bat, distance);
		if (closest == null) {
			return null;
		}
		if (!GeneralUtil.isVampire(closest)) {
			return closest;
		}
		return closestNonVampire(world, bat, distance);
	}

	/**
	 * Stock hang checks use {@link Block#isNormalCube()}, which rejects leaves
	 * and many castle stairs/slabs. Treat any solid non-air material as hangable.
	 */
	@Redirect(
			method = "updateAITasks",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/block/Block;isNormalCube()Z"
			)
	)
	private boolean allowSolidHangSurface(Block block) {
		return canHangUnder(block);
	}

	@Inject(method = "updateAITasks", at = @At("TAIL"))
	private void calmNearVampire(CallbackInfo ci) {
		EntityBat bat = (EntityBat) (Object) this;
		if (this.worldObj == null || this.worldObj.isRemote) {
			return;
		}
		if (bat.getIsBatHanging()) {
			return;
		}
		if (!vampireNearby(bat, CALM_RANGE)) {
			return;
		}

		int x = MathHelper.floor_double(this.posX);
		int y = (int) this.posY + 1;
		int z = MathHelper.floor_double(this.posZ);
		if (canHangUnder(this.worldObj.getBlock(x, y, z)) && this.rand.nextInt(20) == 0) {
			bat.setIsBatHanging(true);
			return;
		}

		// Nudge toward a nearby solid ceiling without touching private spawnPosition.
		if (this.ticksExisted % 20 == 0) {
			nudgeTowardHangSpot();
		}
	}

	private void nudgeTowardHangSpot() {
		int bx = MathHelper.floor_double(this.posX);
		int by = MathHelper.floor_double(this.posY);
		int bz = MathHelper.floor_double(this.posZ);
		for (int i = 0; i < 12; i++) {
			int x = bx + this.rand.nextInt(7) - 3;
			int y = by + this.rand.nextInt(6) - 1;
			int z = bz + this.rand.nextInt(7) - 3;
			if (y < 1) {
				continue;
			}
			if (this.worldObj.isAirBlock(x, y, z) && canHangUnder(this.worldObj.getBlock(x, y + 1, z))) {
				double dx = (double) x + 0.5D - this.posX;
				double dy = (double) y + 0.1D - this.posY;
				double dz = (double) z + 0.5D - this.posZ;
				this.motionX += (Math.signum(dx) * 0.5D - this.motionX) * 0.10000000149011612D;
				this.motionY += (Math.signum(dy) * 0.699999988079071D - this.motionY) * 0.10000000149011612D;
				this.motionZ += (Math.signum(dz) * 0.5D - this.motionZ) * 0.10000000149011612D;
				return;
			}
		}
	}

	private static boolean canHangUnder(Block block) {
		return block != null && block.getMaterial() != null && block.getMaterial().isSolid();
	}

	@SuppressWarnings("unchecked")
	private static boolean vampireNearby(Entity bat, double distance) {
		double maxDistSq = distance * distance;
		List<EntityPlayer> players = bat.worldObj.playerEntities;
		for (int i = 0; i < players.size(); i++) {
			EntityPlayer player = players.get(i);
			if (player == null || !GeneralUtil.isVampire(player)) {
				continue;
			}
			if (player.getDistanceSqToEntity(bat) <= maxDistSq) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static EntityPlayer closestNonVampire(World world, Entity bat, double distance) {
		EntityPlayer best = null;
		double bestDistSq = distance < 0.0D ? Double.MAX_VALUE : distance * distance;
		List<EntityPlayer> players = world.playerEntities;
		for (int i = 0; i < players.size(); i++) {
			EntityPlayer player = players.get(i);
			if (player == null || GeneralUtil.isVampire(player)) {
				continue;
			}
			double distSq = player.getDistanceSqToEntity(bat);
			if (distSq < bestDistSq) {
				bestDistSq = distSq;
				best = player;
			}
		}
		return best;
	}
}
