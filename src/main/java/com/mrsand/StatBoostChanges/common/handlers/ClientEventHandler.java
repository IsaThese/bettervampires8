package com.mrsand.StatBoostChanges.common.handlers;

import org.lwjgl.input.Keyboard;

import com.mrsand.StatBoostChanges.StatBoostChanges;
import com.mrsand.StatBoostChanges.client.CoffinCamera;
import com.mrsand.StatBoostChanges.client.HypnosisBarHud;
import com.mrsand.StatBoostChanges.common.packets.PacketCoffinPassDay;
import com.mrsand.StatBoostChanges.util.CoffinDebug;
import com.mrsand.StatBoostChanges.util.CoffinPoseDebug;
import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;
import com.mrsand.StatBoostChanges.util.HandRenderDebug;
import com.mrsand.StatBoostChanges.util.HypnosisDebug;
import com.mrsand.StatBoostChanges.util.MistPhaseDebug;
import com.mrsand.StatBoostChanges.util.MorphFormDebug;
import com.mrsand.StatBoostChanges.util.WitcheryHandRenderDebug;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import de.teamlapen.vampirism.entity.player.skills.BatSkill;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

@SideOnly(Side.CLIENT)
public class ClientEventHandler {

	private boolean coffinJumpWasDown;
	private long lastPassDayPacketMs;
	/** Tracks nest membership for a one-shot standing restore on leave. */
	private boolean wasInCoffin;
	/** Re-assert standing eyeH/yOff for several ticks after leave (Legends sticks 0.12). */
	private int leaveStandingTicks;
	/** When true, we already showed "Press Space To Slumber" for this sealed-day nest. */
	private boolean slumberHintShown;

	@SubscribeEvent
	public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
//        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return;
//        PacketVignette.render(event.resolution);
	}

	@SubscribeEvent
	public void onRenderHypnosisBar(RenderGameOverlayEvent.Post event) {
		if (!Loader.isModLoaded("legends")) return;
		HypnosisBarHud.render(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onRenderHandDebugBegin(RenderHandEvent event) {
		HandRenderDebug.onRenderHandBegin(event);
		WitcheryHandRenderDebug.onRenderHand(event);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRenderHandDebugEnd(RenderHandEvent event) {
		HandRenderDebug.onRenderHandEnd(event);
	}

	/**
	 * Pin body facing so the F5 model stays coffin-aligned while camera look stays free.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRenderPlayerNestYOffset(RenderPlayerEvent.Pre event) {
		if (event.entityPlayer == null || !Loader.isModLoaded("legends")) return;
		if (!PlayerHelper.isInCoffin(event.entityPlayer)) return;
		CoffinSleepUtil.pinNestBodyFacing(event.entityPlayer);
	}

	/** After nest yOffset clamp (same LOWEST, registered after). */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRenderPlayerCoffinPose(RenderPlayerEvent.Pre event) {
		if (!CoffinPoseDebug.enabled()) return;
		if (event.entityPlayer != null) {
			CoffinPoseDebug.onRenderPlayer(event.entityPlayer);
		}
	}

	/**
	 * Prefer raw key event so Space still fires even if jump bind state is cleared
	 * while nested.
	 */
	@SubscribeEvent
	public void onKeyInput(InputEvent.KeyInputEvent event) {
		if (!Loader.isModLoaded("legends")) return;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.thePlayer == null || mc.currentScreen != null) return;
		EntityPlayer p = mc.thePlayer;
		if (!PlayerHelper.isInCoffin(p)) return;

		KeyBinding jump = mc.gameSettings.keyBindJump;
		boolean jumpPressed = jump.isPressed() || Keyboard.isKeyDown(jump.getKeyCode());
		if (!jumpPressed) return;

		CoffinDebug.log(p, "KEY Space while nested " + CoffinSleepUtil.describeNestState(p)
				+ " closed=" + CoffinSleepUtil.isInClosedLegendsCoffin(p));
		trySendPassDay(p);
	}

	/** Before other LivingUpdate handlers: snapshot Y for leave-lift probe. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onLivingUpdateLeaveLiftStart(LivingUpdateEvent event) {
		if (event.entityLiving == null || !(event.entityLiving instanceof EntityPlayer)) return;
		EntityPlayer p = (EntityPlayer) event.entityLiving;
		if (p.worldObj == null || !p.worldObj.isRemote) return;
		CoffinPoseDebug.noteLeaveLiftLivingStart(p);
	}

	/** After entity update: leave-lift probe + keep leave-lock AABB/pos. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingUpdateLeaveLock(LivingUpdateEvent event) {
		if (event.entityLiving == null || !(event.entityLiving instanceof EntityPlayer)) return;
		if (!Loader.isModLoaded("legends")) return;
		EntityPlayer p = (EntityPlayer) event.entityLiving;
		if (p.worldObj == null || !p.worldObj.isRemote) return;
		CoffinPoseDebug.noteLeaveLiftLivingEnd(p);
		if (!CoffinSleepUtil.isClientLeaveLocked(p) && !CoffinSleepUtil.isClientLeaveYProtect(p)) return;
		if (MorphHelper.isMorphed(p)) return;
		CoffinSleepUtil.assertClientLeaveLockPose(p);
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc != null && mc.thePlayer != null && Loader.isModLoaded("legends")) {
			EntityPlayer p = mc.thePlayer;
			boolean inCoffin = PlayerHelper.isInCoffin(p);
			// START: nest soft-pin, or leave-lock AABB assert before entity update.
			if (event.phase == TickEvent.Phase.START) {
				if (inCoffin) {
					if (CoffinPoseDebug.enabled()) {
						CoffinPoseDebug.tickDense(p, "TICK_START");
						CoffinPoseDebug.checkNestDesync(p);
						CoffinPoseDebug.checkLeaveDesync(p);
					}
				} else if ((CoffinSleepUtil.isClientLeaveLocked(p) || leaveStandingTicks > 0
						|| CoffinSleepUtil.isPostLeaveSettling(p)
						|| CoffinSleepUtil.isClientLeaveYProtect(p))
						&& !MorphHelper.isMorphed(p)) {
					if (CoffinPoseDebug.enabled()) {
						CoffinPoseDebug.checkLeaveYLift(p);
					}
					CoffinSleepUtil.tickClientPostLeave(p);
					if (CoffinPoseDebug.enabled()) {
						CoffinPoseDebug.tickDense(p, "TICK_START");
						CoffinPoseDebug.checkLeaveDesync(p);
						CoffinPoseDebug.checkNoClipFlip(p);
					}
				}
			}
			if (event.phase != TickEvent.Phase.END) return;
			if (MorphHelper.isBat(p)) {
				float sane = Math.max(0.2F, 0.85F * p.height);
				if (p.eyeHeight < 0.15F || p.eyeHeight > p.height + 0.5F) {
					p.eyeHeight = Math.min(sane, BatSkill.BAT_EYE_HEIGHT);
				}
			}
			// In coffin: Space never exits. Always try pass-day packet; server validates lid/day.
			// Nest soft-pin is START-only (no END setPosition).
			inCoffin = PlayerHelper.isInCoffin(p);
			if (inCoffin) {
				leaveStandingTicks = 0;
				CoffinDebug.tickPose(p);
				if (CoffinPoseDebug.enabled()) {
					CoffinPoseDebug.tickDense(p, "TICK_END");
					CoffinPoseDebug.checkNestDesync(p);
					CoffinPoseDebug.checkLeaveDesync(p);
				}
				updateSlumberHint(p);
				KeyBinding jump = mc.gameSettings.keyBindJump;
				boolean jumpDown = jump.getIsKeyPressed() || Keyboard.isKeyDown(jump.getKeyCode());
				if (jumpDown && !coffinJumpWasDown) {
					CoffinDebug.log(p, "TICK Space edge " + CoffinSleepUtil.describeNestState(p)
							+ " closed=" + CoffinSleepUtil.isInClosedLegendsCoffin(p));
					trySendPassDay(p);
				}
				// Suppress vanilla jump while nested (Space must not eject).
				KeyBinding.setKeyBindState(jump.getKeyCode(), false);
				coffinJumpWasDown = jumpDown;
				// Only coffin left/right-click: suppress attack/use unless looking at a Legends coffin.
				if (!isLookingAtLegendsCoffin(mc)) {
					KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
					KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
				}
			} else {
				coffinJumpWasDown = false;
				slumberHintShown = false;
				// Leave edge: offsets + noClip hold; leaveNestPhysics already snapped stand Y.
				if (wasInCoffin) {
					CoffinCamera.onLeave(p);
					leaveStandingTicks = CoffinSleepUtil.POST_LEAVE_STANDING_TICKS;
					if (!MorphHelper.isMorphed(p)) {
						CoffinSleepUtil.applyPostLeaveStanding(p);
					}
				} else if ((leaveStandingTicks > 0 || CoffinSleepUtil.isClientLeaveLocked(p)
						|| CoffinSleepUtil.isClientLeaveYProtect(p))
						&& !MorphHelper.isMorphed(p)) {
					if (leaveStandingTicks > 0) {
						leaveStandingTicks--;
						if (leaveStandingTicks <= 0) {
							int extra = CoffinSleepUtil.tryExtendClientLeaveIfHigh(p);
							if (extra > 0) {
								leaveStandingTicks = extra;
							}
						}
					}
					CoffinSleepUtil.tickClientPostLeave(p);
				}
				if (CoffinPoseDebug.enabled() && (wasInCoffin || leaveStandingTicks > 0
						|| CoffinSleepUtil.isPostLeaveSettling(p)
						|| CoffinSleepUtil.isClientLeaveLocked(p)
						|| CoffinSleepUtil.isClientLeaveYProtect(p))) {
					CoffinPoseDebug.tickDense(p, "TICK_END");
					CoffinPoseDebug.checkLeaveDesync(p);
					CoffinPoseDebug.checkNoClipFlip(p);
				}
			}
			wasInCoffin = inCoffin;
			CoffinDebug.tickClient(p);
		}
		if (HandRenderDebug.enabled()) {
			HandRenderDebug.announceJvmFlagIfNeeded();
		}
		if (WitcheryHandRenderDebug.enabled()) {
			WitcheryHandRenderDebug.announceIfNeeded();
		}
		if (MorphFormDebug.enabled()) {
			if (mc != null && mc.thePlayer != null) {
				MorphFormDebug.tickPeriodic(mc.thePlayer);
			}
		}
		if (HypnosisDebug.enabled()) {
			if (mc != null && mc.thePlayer != null) {
				HypnosisDebug.announceIfNeeded(mc.thePlayer);
			}
		}
		if (MistPhaseDebug.enabled()) {
			if (mc != null && mc.thePlayer != null) {
				MistPhaseDebug.announceIfNeeded(mc.thePlayer);
			}
		}
		if (CoffinDebug.enabled()) {
			if (mc != null && mc.thePlayer != null) {
				CoffinDebug.announceIfNeeded(mc.thePlayer);
			}
		}
		if (CoffinPoseDebug.enabled()) {
			if (mc != null && mc.thePlayer != null) {
				CoffinPoseDebug.announceIfNeeded(mc.thePlayer);
			}
		}
	}

	private void trySendPassDay(EntityPlayer p) {
		long now = System.currentTimeMillis();
		if (now - lastPassDayPacketMs < 250L) return;
		lastPassDayPacketMs = now;
		CoffinDebug.logImportant(p, "SEND PacketCoffinPassDay");
		StatBoostChanges.network.sendToServer(new PacketCoffinPassDay());
	}

	private static boolean isLookingAtLegendsCoffin(Minecraft mc) {
		if (mc == null || mc.theWorld == null) return false;
		MovingObjectPosition mop = mc.objectMouseOver;
		if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
			return false;
		}
		Block block = mc.theWorld.getBlock(mop.blockX, mop.blockY, mop.blockZ);
		return CoffinSleepUtil.isLegendsCoffinBlock(block);
	}

	/**
	 * Daytime + sealed lid: one chat hint. Night or open lid: no message (hint can show again
	 * next time conditions are met).
	 */
	private void updateSlumberHint(EntityPlayer p) {
		boolean canSlumber = CoffinSleepUtil.isInClosedLegendsCoffin(p) && p.worldObj.isDaytime();
		if (!canSlumber) {
			slumberHintShown = false;
			return;
		}
		if (slumberHintShown) return;
		slumberHintShown = true;
		p.addChatComponentMessage(new ChatComponentText("Press Space To Slumber")
				.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.DARK_PURPLE).setItalic(true)));
	}
}
