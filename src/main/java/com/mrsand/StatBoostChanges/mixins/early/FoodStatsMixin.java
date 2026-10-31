package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.world.EnumDifficulty;

@Mixin(FoodStats.class)
public abstract class FoodStatsMixin {
    @Shadow(remap = false) private int field_75127_a;
    @Shadow(remap = false) private float field_75125_b;
    @Shadow(remap = false) private float field_75126_c;
    @Shadow(remap = false) private int field_75123_d;
    @Shadow(remap = false) private int field_75124_e;

    @Shadow(remap = false)
    public abstract void func_75113_a(float p_75113_1_);

    @Inject(method = "func_75118_a(Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onUpdateChange(EntityPlayer player, CallbackInfo ci)
    {
        ci.cancel();
//        GeneralUtil.sendDebugMessage(player, "canceled original hunger mechanics");

        EnumDifficulty difficulty = player.worldObj.difficultySetting;
        this.field_75124_e = this.field_75127_a;

        // 1. EXHAUSTION & HUNGER DEPLETION
        // Every time exhaustion passes 4.0, it reduces either saturation or actual food level.
        if (this.field_75126_c > 4.0F)
        {
            this.field_75126_c -= 4.0F;

            if (this.field_75125_b > 0.0F)
            {
                // Reduce saturation first if the player has any
                this.field_75125_b = Math.max(this.field_75125_b - 1.0F, 0.0F);
            }
            else if (difficulty != EnumDifficulty.PEACEFUL)
            {
                // If no saturation remains and game is not on Peaceful, reduce the hunger bar
                this.field_75127_a = Math.max(this.field_75127_a - 1, 0);
            }
        }

        // 2. NATURAL REGENERATION (HEALING)
        // If the naturalRegen gamerule is true, hunger is 18 or higher (9+ shanks), and the player needs healing
        boolean hasNaturalRegen = player.worldObj.getGameRules().getGameRuleBooleanValue("naturalRegeneration");
        if (hasNaturalRegen && this.field_75127_a >= 18 && player.shouldHeal())
        {
            ++this.field_75123_d;

            // Every 80 ticks (4 seconds)
            if (this.field_75123_d >= 80)
            {
                player.heal(1.0F);         // Heals half a heart
                this.func_75113_a(3.0F);   // Healing costs 3.0 points of exhaustion
                this.field_75123_d = 0;
            }
        }
        // 3. STARVATION DAMAGE
        // If the hunger bar is completely empty
        else if (this.field_75127_a <= 0)
        {
            ++this.field_75123_d;

            // Every 80 ticks (4 seconds)
            if (this.field_75123_d >= 80)
            {
                // Difficulty checks:
                // - EASY: Only damages if health is above 10.0F (5 hearts)
                // - NORMAL: Only damages if health is above 1.0F (half a heart)
                // - HARD: Always damages (can kill the player)
                if (player.getHealth() > 10.0F || difficulty == EnumDifficulty.HARD || (player.getHealth() > 1.0F && difficulty == EnumDifficulty.NORMAL))
                {
                    if(!GeneralUtil.isVampire(player)){
                        player.attackEntityFrom(DamageSource.starve, 1.0F); // Deals half a heart of starvation damage
                    }
                    else if(player.getHealth() > 1.0F) {
                        if(Loader.isModLoaded("witchery") && WitcheryUtil.getBloodLevel(player) > 0) {
                            player.attackEntityFrom(DamageSource.starve, 1.0F); 
                        }
                        else {
                            player.attackEntityFrom(DamageSource.starve, 1.0F); 
                        }
                    }
                }

                this.field_75123_d = 0;
            }
        }
        else
        {
            // Reset the timer if the player is neither starving nor actively healing
            this.field_75123_d = 0;
        }
    }
}