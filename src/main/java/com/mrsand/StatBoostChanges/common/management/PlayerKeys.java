package com.mrsand.StatBoostChanges.common.management;

public enum PlayerKeys {
	IS_STARVING,
	COUNTER_STARVATION,
	HAS_STARVED,
	IS_VULNERABLE,
	TIMER_FORGETVAMPIRE,
	SUSPICION,
	HYPNOTIZING,
	WALL_CLIMB,
	OTHERFORM,
	SECONDSLOT,
	LOCATION,
	TIMER_STARVATION, SLEEP_START, LOCATION_LAST_CHANGE_TICK, COUNTER_MORPH,
	/** Auto-granted restored abilities unlocked for vampire legacy. */
	UNLOCKED_RESTORED_POWERS,
	/** Timed Demonic Fury (attack blood drain only). */
	DEMONIC_FURY_TICKS,
	/** Hypnosis Mastery resource (replaces removed PlayerHelper hypnosis strength). */
	HYPNOSIS_STRENGTH,
	/** Post-coffin-leave: server owns leave-stand coords (synced via VExtendedPlayerPacket). */
	LEAVE_STANDING
}
