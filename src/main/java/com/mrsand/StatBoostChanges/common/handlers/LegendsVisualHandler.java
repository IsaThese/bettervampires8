package com.mrsand.StatBoostChanges.common.handlers;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mrsand.StatBoostChanges.ModCompat; // Imported your new class
import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.VisualUtil;
import com.tihyo.horror.legacy.suits.LegacyVampireApparelV1;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;

public class LegendsVisualHandler {

    private final ItemStack[] EMPTY = new ItemStack[0];
    private static final ResourceLocation RED_EYES_TEXTURE = new ResourceLocation("bettervampires", "textures/overlay_red_eyes.png");
    private static final ResourceLocation OTHER_SKIN = new ResourceLocation("bettervampires", "textures/otherskin.png");

    // Performance Optimization: Cache static instances of armor to stop garbage generation
    private static ItemStack vampireChest;
    private static ItemStack vampireLegs;
    private static ItemStack vampireBoots;
    private static boolean armorInitialized = false;

    private final LoadingCache<EntityPlayer, ItemStack[]> armorCache = CacheBuilder.newBuilder()
            .expireAfterAccess(60L, TimeUnit.SECONDS)
            .build(new CacheLoader<EntityPlayer, ItemStack[]>() {
                public ItemStack[] load(EntityPlayer owner) { return EMPTY; }
            });

    /**
     * Safely checks and instantiates custom vampire armor items exactly once.
     * Reuses these instances continuously to save significant CPU cycles.
     */
    private void initializeVampireArmor() {
        if (!armorInitialized) {
            if (LegacyVampireApparelV1.chest != null) vampireChest = new ItemStack(LegacyVampireApparelV1.chest);
//            if (LegacyVampireApparelV1.legs != null) vampireLegs = new ItemStack(LegacyVampireApparelV1.legs);
//            if (LegacyVampireApparelV1.boots != null) vampireBoots = new ItemStack(LegacyVampireApparelV1.boots);
            armorInitialized = true;
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.entityPlayer;
        
        // Fast-fail: If player is missing or render is already skipped/canceled, exit instantly
        if (player == null || event.isCanceled()) return;

        if (GeneralUtil.isVampire(player)) {
            VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
            if (vPlayer == null) return;
            
            if (vPlayer.getBoolean(PlayerKeys.HYPNOTIZING) || vPlayer.getBoolean(PlayerKeys.IS_STARVING) || LegendsUtil.bloodVision(player)) {
                VisualUtil.changeForm(player, true);
            }

            // Day/night Legends apparel force-swap (config: enableDayNightVampireApparel)
            if (!StatBoostConfig.enableDayNightVampireApparel) return;

            // Prepare static armor pieces if not already initialized
            initializeVampireArmor();

            ItemStack[] realArmor = player.inventory.armorInventory;
            this.armorCache.put(player, Arrays.copyOf(realArmor, realArmor.length));

            // Same sunlight helper as BatSkill (light + sun-safe biome + rain), not crude worldTime
            boolean inSunlight = false;
            if (ModCompat.VAMPIRISM) {
                VampirePlayer vp = VampirePlayer.get(player);
                inSunlight = vp != null && vp.isInSunlight();
            }

            for (int i = 0; i < realArmor.length; i++) {
                if (inSunlight) {
                    realArmor[i] = null;
                } else {
                    // Performance Fix: Reuses static ItemStacks instead of calling "new" 60+ times a second
                    if (i == 3) realArmor[i] = null; 
                    if (i == 2) realArmor[i] = vampireChest;
                    if (i == 1) realArmor[i] = vampireLegs;
                    if (i == 0) realArmor[i] = vampireBoots;
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!StatBoostConfig.enableDayNightVampireApparel) return;
        if (event.entityPlayer != null) {
            restoreArmor(event.entityPlayer);
        }
    }

    private void restoreArmor(EntityPlayer player) {
        ItemStack[] cached = armorCache.getUnchecked(player);
        if (cached != EMPTY) {
            ItemStack[] currentArmor = player.inventory.armorInventory;
            for (int i = 0; i < cached.length; i++) {
                currentArmor[i] = cached[i];
            }
            armorCache.put(player, EMPTY);
        }
    }
}