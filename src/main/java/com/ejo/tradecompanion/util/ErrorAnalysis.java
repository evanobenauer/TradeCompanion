package com.ejo.tradecompanion.util;

public class ErrorAnalysis {

    public static boolean isWithinDifference(float val1, float val2, double maxPercentDifference) {
        double avg = (val1 + val2) / 2; //AVG returns zero if BOTH val are 0 OR if val1 == -val2
        if (avg == 0) return val1 == 0; //This protects against infinite percent error for 0 values due to /0 error
        double percentDiff = Math.abs((val1 - val2) / avg);
        return percentDiff < maxPercentDifference;
    }

    public static boolean isMagnitudeWithinDifference(float val1, float val2, double maxPercentDifference) {
        return isWithinDifference(Math.abs(val1),Math.abs(val2),maxPercentDifference);
    }

    public static boolean isWithinError(float trueValue, float experimentalValue, double maxPercentError) {
        double percentDiff = Math.abs((experimentalValue - trueValue) / trueValue);
        return percentDiff < maxPercentError;
    }

    public static boolean isMagnitudeWithinError(float trueValue, float experimentalValue, double maxPercentError) {
        return isWithinError(Math.abs(trueValue),Math.abs(experimentalValue),maxPercentError);
    }
}
