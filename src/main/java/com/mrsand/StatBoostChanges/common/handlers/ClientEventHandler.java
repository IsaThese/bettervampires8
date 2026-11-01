package com.mrsand.StatBoostChanges.common.handlers;

import com.mrsand.StatBoostChanges.client.HypnosisBarHud;
import com.mrsand.StatBoostChanges.util.HandRenderDebug;
import com.mrsand.StatBoostChanges.util.HypnosisDebug;
import com.mrsand.StatBoostChanges.util.MistPhaseDebug;
import com.mrsand.StatBoostChanges.util.MorphFormDebug;
import com.mrsand.StatBoostChanges.util.WitcheryHandRenderDebug;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;

@SideOnly(Side.CLIENT)
public class ClientEventHandler {

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

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (HandRenderDebug.enabled()) {
			HandRenderDebug.announceJvmFlagIfNeeded();
		}
		if (WitcheryHandRenderDebug.enabled()) {
			WitcheryHandRenderDebug.announceIfNeeded();
		}
		if (MorphFormDebug.enabled()) {
			Minecraft mc = Minecraft.getMinecraft();
			if (mc != null && mc.thePlayer != null) {
				MorphFormDebug.tickPeriodic(mc.thePlayer);
			}
		}
		if (HypnosisDebug.enabled()) {
			Minecraft mc = Minecraft.getMinecraft();
			if (mc != null && mc.thePlayer != null) {
				HypnosisDebug.announceIfNeeded(mc.thePlayer);
			}
		}
		if (MistPhaseDebug.enabled()) {
			Minecraft mc = Minecraft.getMinecraft();
			if (mc != null && mc.thePlayer != null) {
				MistPhaseDebug.announceIfNeeded(mc.thePlayer);
			}
		}
	}
}
