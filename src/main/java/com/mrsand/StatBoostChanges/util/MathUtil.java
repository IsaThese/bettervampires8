package com.mrsand.StatBoostChanges.util;

public class MathUtil {
	
	public double getTriggerProbability(int trials) {
	    double k = 0.8;      // Steepness
	    double n0 = 8.6;     // Midpoint (50% chance)
	    
	    return 1.0 / (1.0 + Math.exp(-k * (trials - n0)));
	}

}
