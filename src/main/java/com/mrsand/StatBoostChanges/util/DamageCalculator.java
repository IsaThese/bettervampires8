package com.mrsand.StatBoostChanges.util;

import com.ibm.icu.impl.Assert;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;


public class DamageCalculator {

    /*
    Use as an @ovveride for attackedEntityAsMob(EntityLivingBase entity).

     */
    public static float pierceArmorMaxDiamond(EntityLivingBase entity, EntityLivingBase attacker){
        if (entity == null || attacker == null) {
            return 0F;
        }

        int targetArmor = entity.getTotalArmorValue();
        float armorPiercingDamage = 0F;

        if (targetArmor > 50) {
            armorPiercingDamage = 1.8F * (float) Math.log(targetArmor - 19.0F);
            armorPiercingDamage = Math.round(armorPiercingDamage * 2.0F) / 2.0F;

            entity.hurtResistantTime = 0;

            entity.attackEntityFrom(DamageSource.causeMobDamage(attacker).setDamageBypassesArmor().setMagicDamage(), armorPiercingDamage);
        }
        return armorPiercingDamage;
    }

    /*
    Caps damage above a diamond sword 13.5. Ex: //13.5 = 13.5, 40->15.5, rounds to nearest half, 750 ->18
    To be used in @Override public boolean attackEntityFrom(DamageSource p_70097_1_, float p_70097_2_).
     */
    public static float capDamage(float damage){
        if (damage > 13.5F) {
            float rawDamage = 13.5F + 0.72F * (float) Math.log(((damage - 13.5F) / 1.6F) + 1.0F);
            damage = Math.round(rawDamage * 2.0F) / 2.0F;
        }
        return damage;
    }

    /**
     * True when Vampire Lord clothes soft-cap may apply: living attacker, not
     * fire/starve/drown/void/sun.
     */
    public static boolean shouldCapVampireLordIncoming(DamageSource source) {
        if (source == null) {
            return false;
        }
        if (!(source.getEntity() instanceof EntityLivingBase)) {
            return false;
        }
        if (source.isFireDamage()) {
            return false;
        }
        if (source == DamageSource.starve || source == DamageSource.drown || source == DamageSource.outOfWorld) {
            return false;
        }
        // VampirismMod.sunDamage uses damageType "sun"
        if ("sun".equals(source.damageType)) {
            return false;
        }
        return true;
    }
}
