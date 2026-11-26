package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;
import com.mrsand.StatBoostChanges.common.packets.PacketClearDeathScreen;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LordColossalCombat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Player path: blocks OreSpawn-style {@code setHealth} HP rollbacks outside combat.
 * Mob path: after a Vampire Lord hit, blocks upward {@code setHealth}/{@code heal}
 * for the engagement anti-heal window.
 * Client: when a vampire's HP is set to &lt;= 0, pre-arm GuiGameOver suppress.
 */
@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseSetHealthGuardMixin {

	/** {@code setHealth} (srg {@code func_70606_j}). */
	@Inject(method = "func_70606_j", at = @At("HEAD"), cancellable = true)
	private void bvGuardSetHealth(float health, CallbackInfo ci) {
		EntityLivingBase self = (EntityLivingBase) (Object) this;
		if (self.worldObj != null && self.worldObj.isRemote) {
			// Client only: arm death-GUI suppress before runTick opens GuiGameOver.
			if (self instanceof EntityPlayer && health <= 0.0F) {
				EntityPlayer player = (EntityPlayer) self;
				boolean vamp = false;
				try {
					vamp = GeneralUtil.isVampire(player);
				} catch (Throwable ignored) {
				}
				if (vamp) {
					PacketClearDeathScreen.armSuppressClient(25);
					// #region agent log
					try {
						java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(
								"c:/Users/isaac/IdeaProjects/bettervampires8/.cursor/debug-9ecb1a.log", true));
						pw.println("{\"sessionId\":\"9ecb1a\",\"runId\":\"post-fix\",\"hypothesisId\":\"H1\","
								+ "\"location\":\"EntityLivingBaseSetHealthGuardMixin\",\"message\":\"client_arm_suppress\","
								+ "\"timestamp\":" + System.currentTimeMillis()
								+ ",\"data\":{\"newHp\":" + health + ",\"curHp\":" + player.getHealth()
								+ ",\"suppress\":" + PacketClearDeathScreen.getSuppressTicks() + "}}");
						pw.close();
					} catch (Throwable ignored) {
					}
					// #endregion
				}
			}
			return;
		}

		if (self instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) self;
			float cur = player.getHealth();
			if (!(health < cur)) return;
			// Real combat: LivingHurt armed an expected drop for this player.
			if (LordColossalCombat.consumeExpectedHealthDrop(player)) return;
			if (LordColossalCombat.isAllowDirectHealthDrop()) return;

			boolean invuln = LordColossalCombat.isDeathSaveInvulnerable(player);
			boolean lord = false;
			try {
				lord = DaywalkerControl.isVampireLord(player);
			} catch (Throwable ignored) {
			}
			if (!invuln && !lord) return;

			ci.cancel();
			return;
		}

		// Mob engagement anti-heal: cancel HP increases only.
		if (LordColossalCombat.shouldBlockMobHealthRaise(self, health)) {
			ci.cancel();
		}
	}
}
