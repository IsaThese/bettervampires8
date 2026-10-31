package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;

import cpw.mods.fml.common.Loader;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.proxy.IProxy;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = VampirePlayer.BloodStats.class, remap = false)
public abstract class VampirePlayerMixin {

//	@Redirect(
//		    method = "onUpdate",
//		    at = @At(
//		        value = "INVOKE",
//		        target = "Lde/teamlapen/vampirism/proxy/IProxy;setPlayerBat(Lnet/minecraft/entity/player/EntityPlayer;Z)V",
//		        remap = false
//		    ),
//		    require = 1,
//		    remap = false
//		)
//	public void redirectSetPlayerBat(IProxy proxy, EntityPlayer player, boolean bat) {
//		GeneralUtil.sendDebugMessage(player, "custom bat Method");
//		if (!player.worldObj.isRemote && Loader.isModLoaded("legends")) {
//			LegendsUtil.morphBat(player);
//		}
//	}
}
