package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.GeneralUtil;

import cpw.mods.fml.common.Loader;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer {
//
//    private static final ResourceLocation VAMPIRE_CAPE = new ResourceLocation("bettervampires", "textures/vampire_cape.png");
//
//    private static boolean enableCape = false ? Loader.isModLoaded("Cape"): false;
//
////     1. Force vanilla to acknowledge the player has a cape, has cape or func_152122_n
//    @Inject(method = "func_152122_n", at = @At("HEAD"), cancellable = true)
//    private void injectVampireHasCape(CallbackInfoReturnable<Boolean> cir) {
//        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
//
//        if (GeneralUtil.isVampire(player) && !GeneralUtil.isDay(player) && enableCape) {
////        	GeneralUtil.sendDebugMessage(player, "YOU HAVE A CAPE?!");
//            cir.setReturnValue(true);
//        }
//    }
//
//    // 2. Give vanilla the actual texture to use get Location Cape
//    @Inject(method = "func_110303_q", at = @At("HEAD"), cancellable = true)
//    private void injectVampireCapeLocation(CallbackInfoReturnable<ResourceLocation> cir) {
//        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
//
//        if (GeneralUtil.isVampire(player) && enableCape) {
////        	GeneralUtil.sendDebugMessage(player, "FOUND CAPE?!");
//            cir.setReturnValue(VAMPIRE_CAPE);
//        }
//    }
}
