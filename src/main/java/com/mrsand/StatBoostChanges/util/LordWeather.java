package com.mrsand.StatBoostChanges.util;

import java.util.List;
import java.util.Random;

import com.mrsand.StatBoostChanges.abilities.VampireSunRules;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;

/**
 * Vampire Lord weather toggle — same WorldInfo mutations as vanilla
 * {@code /weather thunder} and {@code /weather clear} (CommandWeather),
 * plus a session flag so daylight mist can track lord-caused storms.
 */
public final class LordWeather {

	public static final String NBT_LORD_STORM = "bvLordStorm";
	/** Matches Herobrine event fog: duration ticks, amplifier. */
	public static final int FOG_DURATION = 100;
	public static final int FOG_AMPLIFIER = 2;
	private static final double MIST_RADIUS = 64.0D;

	/** Session flag: weather is overworld-global like CommandWeather. */
	private static boolean lordStormActive;

	private LordWeather() {}

	public static boolean isLordStormActive(World world) {
		return lordStormActive;
	}

	public static void setLordStormActive(World world, boolean active) {
		lordStormActive = active;
	}

	/** True if overworld WorldInfo is raining or thundering (vanilla weather state). */
	public static boolean isStorming(World world) {
		WorldInfo info = getOverworldInfo();
		return info != null && (info.isRaining() || info.isThundering());
	}

	/**
	 * Toggle like {@code /weather thunder} ↔ {@code /weather clear}.
	 * @return true if storm is now on, false if cleared
	 */
	public static boolean toggleStorm(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) {
			return false;
		}
		WorldInfo info = getOverworldInfo();
		if (info == null) return false;

		if (info.isRaining() || info.isThundering() || lordStormActive) {
			clearStorm(player.worldObj);
			clearFog(player);
			return false;
		}
		startStorm(player.worldObj, player);
		applyFogBurst(player);
		return true;
	}

	/**
	 * Same as CommandWeather {@code /weather thunder}: set rain+thunder times on
	 * overworld WorldInfo only. No strength force, no lightning bolt.
	 */
	public static void startStorm(World world, EntityPlayer caster) {
		if (world == null || world.isRemote) return;
		WorldInfo info = getOverworldInfo();
		if (info == null) return;

		// CommandWeather: (300 + rand(600)) * 20 ticks
		int duration = (300 + (new Random()).nextInt(600)) * 20;
		info.setRainTime(duration);
		info.setThunderTime(duration);
		info.setRaining(true);
		info.setThundering(true);

		lordStormActive = true;
	}

	/**
	 * Same as CommandWeather {@code /weather clear}.
	 */
	public static void clearStorm(World world) {
		if (world == null || world.isRemote) return;
		WorldInfo info = getOverworldInfo();
		if (info == null) return;

		info.setRainTime(0);
		info.setThunderTime(0);
		info.setRaining(false);
		info.setThundering(false);

		lordStormActive = false;
	}

	/** Overworld WorldInfo — same target as {@code CommandWeather.processCommand}. */
	private static WorldInfo getOverworldInfo() {
		MinecraftServer server = MinecraftServer.getServer();
		if (server == null || server.worldServers == null || server.worldServers.length == 0) {
			return null;
		}
		return server.worldServers[0].getWorldInfo();
	}

	public static void applyFogBurst(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (!Loader.isModLoaded("legends")) return;
		try {
			com.tihyo.legends.effects.LegendsEffects.addFog(player, FOG_DURATION, FOG_AMPLIFIER);
		} catch (Throwable ignored) {
		}
	}

	public static void clearFog(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (!Loader.isModLoaded("legends")) return;
		try {
			int id = com.tihyo.legends.effects.LegendsEffects.fog.id;
			if (player.isPotionActive(id)) {
				player.removePotionEffect(id);
			}
		} catch (Throwable ignored) {
		}
	}

	/**
	 * While lord-storm is active and it is calendar daytime for this player,
	 * keep Herobrine-style fog up (EntityHerobrine reapplies addFog(100, 2) every
	 * tick for nearby players — continuous renew, not a 2s pulse).
	 * Also clears the flag if the storm has ended naturally.
	 */
	public static void tickDaylightMist(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		World world = player.worldObj;
		if (!lordStormActive) return;

		// Drop flag if overworld weather ended without a lord clear.
		if (!isStorming(world)) {
			lordStormActive = false;
			return;
		}

		if (!Loader.isModLoaded("legends")) return;
		if (!VampireSunRules.isDaytime(player)) return;

		// Only the storm caster / one player should drive AOE refresh per tick:
		// re-apply continuously like Herobrine (no %40 pulse). Skip if fog already
		// has plenty of time left to avoid redundant potion replaces when many players tick.
		applyFogIfNeeded(player);

		@SuppressWarnings("unchecked")
		List<EntityPlayer> nearby = world.getEntitiesWithinAABB(
				EntityPlayer.class,
				player.boundingBox.expand(MIST_RADIUS, MIST_RADIUS, MIST_RADIUS));
		for (EntityPlayer other : nearby) {
			if (other == null || other == player) continue;
			if (other.capabilities != null && other.capabilities.isCreativeMode) continue;
			applyFogIfNeeded(other);
		}
	}

	/**
	 * Herobrine uses addFog(100, 2) while nearby. Renewing only when duration is low
	 * keeps fog continuous without the jarring ~2s re-apply pulse.
	 */
	private static void applyFogIfNeeded(EntityPlayer player) {
		if (player == null) return;
		try {
			PotionEffect active = player.getActivePotionEffect(com.tihyo.legends.effects.LegendsEffects.fog);
			if (active != null && active.getDuration() > 80) {
				return;
			}
		} catch (Throwable ignored) {
			// If we cannot read remaining duration, fall through and renew.
		}
		applyFogBurst(player);
	}
}
