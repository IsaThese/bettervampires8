package com.mrsand.StatBoostChanges.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.emoniph.witchery.item.ItemVampireClothes;
import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.client.WitcheryApparelRender;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderHandEvent;

/**
 * Opt-in FP hand probe for Witchery vampire clothes.
 * Enable: {@code -Dstatboo.debugWitcheryHand=true}
 */
@SideOnly(Side.CLIENT)
public final class WitcheryHandRenderDebug {

	public static final Logger LOG = LogManager.getLogger("StatBoo/WitcheryHand");

	private static final boolean ENABLED =
			Boolean.parseBoolean(System.getProperty("statboo.debugWitcheryHand", "false"));

	private static final Map<String, Long> lastLogByKey = new HashMap<String, Long>();
	private static boolean announced;

	private WitcheryHandRenderDebug() {}

	public static boolean enabled() {
		return ENABLED;
	}

	public static boolean shouldProbe() {
		if (!ENABLED || !Loader.isModLoaded("witchery")) return false;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0) return false;
		return wearsAnyVampireClothes(mc.thePlayer);
	}

	public static boolean wearsAnyVampireClothes(EntityPlayer player) {
		if (player == null) return false;
		ItemStack[] armor = player.inventory.armorInventory;
		for (int i = 0; i < 4; i++) {
			ItemStack s = armor[i];
			if (s != null && s.getItem() instanceof ItemVampireClothes) {
				return true;
			}
		}
		return false;
	}

	public static void announceIfNeeded() {
		if (!ENABLED || announced) return;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.thePlayer == null) return;
		announced = true;
		LOG.info("Witchery hand debug ON via -Dstatboo.debugWitcheryHand=true");
		chat("Witchery hand debug ON — wear vampire clothes in 1st person");
	}

	public static void logThrottled(String key, String msg) {
		if (!shouldProbe()) return;
		long now = System.currentTimeMillis();
		Long last = lastLogByKey.get(key);
		if (last != null && now - last < 750L) return;
		lastLogByKey.put(key, now);
		LOG.info(msg);
		chat(msg);
	}

	public static void onRenderHand(RenderHandEvent event) {
		if (!shouldProbe()) return;
		announceIfNeeded();
		logThrottled("RenderHandEvent", "RenderHandEvent canceled=" + event.isCanceled()
				+ " dayNightApparel=" + StatBoostConfig.enableDayNightVampireApparel);
	}

	public static void onFirstPersonArm(EntityPlayer player, RenderPlayer render) {
		if (!shouldProbe()) return;
		announceIfNeeded();
		StringBuilder sb = new StringBuilder("FP_ARM clothes=");
		ItemStack[] armor = player.inventory.armorInventory;
		for (int i = 0; i < 4; i++) {
			ItemStack s = armor[i];
			sb.append('[').append(i).append('=');
			if (s == null) sb.append("empty");
			else sb.append(s.getItem().getClass().getSimpleName());
			sb.append(']');
		}
		ModelBiped main = render.modelBipedMain;
		boolean mainArm = main != null && main.bipedRightArm != null && main.bipedRightArm.showModel;
		sb.append(" mainArmShow=").append(mainArm);

		ItemStack chest = player.getCurrentArmor(2);
		if (chest != null && chest.getItem() instanceof ItemVampireClothes) {
			ModelBiped custom = WitcheryApparelRender.getArmorModel(player, chest, 1);
			sb.append(" chestModel=");
			if (custom == null) {
				sb.append("null");
			} else {
				sb.append(custom.getClass().getSimpleName());
				boolean arm = custom.bipedRightArm != null && custom.bipedRightArm.showModel;
				sb.append(" chestArmShow=").append(arm);
			}
		}
		sb.append(" renderClass=").append(render.getClass().getSimpleName());
		logThrottled("FP_ARM", sb.toString());
	}

	/**
	 * Called from Legends {@code RenderPlayerHand} mixin — pass precomputed flags so this
	 * class stays free of a hard Legends compile/runtime dependency.
	 */
	public static void onRenderPlayerHand(EntityPlayer player, boolean shouldRenderHand,
			boolean shouldUseLegendsRender) {
		if (!shouldProbe()) return;
		announceIfNeeded();
		ItemStack chest = player.getCurrentArmor(2);
		String chestName = chest == null || chest.getItem() == null
				? "empty"
				: chest.getItem().getClass().getSimpleName();
		logThrottled("RPH", "RenderPlayerHand chest=" + chestName
				+ " shouldRenderHand=" + shouldRenderHand
				+ " shouldUseLegendsRender=" + shouldUseLegendsRender);
	}

	public static void onItemRenderer() {
		if (!shouldProbe()) return;
		logThrottled("ItemRenderer", "ItemRenderer.renderItemInFirstPerson HEAD");
	}

	private static void chat(String msg) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.thePlayer == null) return;
		String line = "[WHand] " + msg;
		if (line.length() > 120) line = line.substring(0, 117) + "...";
		GeneralUtil.sendDebugMessage(mc.thePlayer, new ChatComponentText(line));
	}
}
