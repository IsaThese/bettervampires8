package de.teamlapen.vampirism.entity.player.skills;

import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LordWeather;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import cpw.mods.fml.common.Loader;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public class ChangeWeatherSkill extends DefaultSkill {

    @Override
	public int getCooldown() {
        int multiplierCooldown = 0;
        return BALANCE.VP_SKILLS.WEATHER_COOLDOWN * multiplierCooldown;
	}

	@Override
	public int getMinLevel() {
		return BALANCE.VP_SKILLS.WEATHER_MIN_LEVEL;
	}

	@Override
	public boolean canBeUsedBy(VampirePlayer vampire, EntityPlayer player) {
		// Lords: night anywhere, or daytime under a roof (not open sky)
		return vampire.isVampireLord() && VampireSunRules.canStartWeather(player);
	}

	@Override
	public int getMinU() {
		return 48;
	}

	@Override
	public int getMinV() {
		return 0;
	}

	@Override
	public String getUnlocalizedName() {
		return "skill.vampirism.change_weather";
	}

	@Override
	public boolean onActivated(VampirePlayer vampire, EntityPlayer player) {
		if (Loader.isModLoaded("witchery")) {
			if (WitcheryUtil.hasBloodPower(player, 0, .5F)) {
				// This is intentional
				WitcheryUtil.decreaseBlood(player, 0, .05F, true);
			} else {
				GeneralUtil.sendDebugMessage(player, new ChatComponentText("You don't have enough power, need more than half")
						.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				return false;
			}
		}
		// Storm on ↔ fully clear; fog burst on start, cleared on clear
		LordWeather.toggleStorm(player);
		return true;
	}

}
