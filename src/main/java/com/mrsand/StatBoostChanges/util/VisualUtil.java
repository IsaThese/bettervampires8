package com.mrsand.StatBoostChanges.util;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

public class VisualUtil {

	/**
	 * Marks power-eyes / hostility otherform without swapping the player skin.
	 * Eye overlay is drawn by Legends {@code PlayerRenderHelper} under BV eye modes.
	 */
	public static void changeForm(EntityPlayer player, Boolean changeToOtherForm) {
		if (player == null) return;
		VExtendedPlayer props = VExtendedPlayer.get(player);
		if (props == null) return;
		if (props.getBoolean(PlayerKeys.IS_STARVING) || props.getBoolean(PlayerKeys.HYPNOTIZING)
				|| LegendsUtil.bloodVision(player)) {
			changeToOtherForm = true;
		}

		// Callers sync (begin/endHypnosisChannel, packet handler). Do not sync here —
		// auto syncClient fought Morph/regen floods and double-fired with those callers.
		props.setBoolean(PlayerKeys.OTHERFORM, changeToOtherForm.booleanValue());
	}

	public static void updateVillageSidebar(EntityPlayer player, int distance) {
	    Scoreboard scoreboard = player.getWorldScoreboard();
	    
	    // 1. Find or create the "Village" objective
	    ScoreObjective objective = scoreboard.getObjective("VillageTracker");
	    if (objective == null) {
	        objective = scoreboard.addScoreObjective("VillageTracker", IScoreObjectiveCriteria.field_96641_b);
	        objective.setDisplayName(EnumChatFormatting.GOLD + "NEAREST VILLAGE");
	        
	        // 2. Set the display slot to the Sidebar (id 1)
	        scoreboard.func_96530_a(1, objective); 
	    }

	    // 3. Update the distance value for the player
	    // This looks like: [PlayerName] ... [Distance]
	    Score score = scoreboard.func_96529_a(player.getCommandSenderName(), objective);
	    score.setScorePoints(distance); 
	}
}
