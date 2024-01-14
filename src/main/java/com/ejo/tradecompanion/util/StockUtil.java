package com.ejo.tradecompanion.util;

import com.ejo.glowlib.time.DateTime;

public class StockUtil {

    public static boolean isTradingHours(DateTime currentTime) {
        return !currentTime.isWeekend() && currentTime.getHour() < 16 && currentTime.getHour() >= 9 && (currentTime.getHour() != 9 || currentTime.getMinute() >= 30);
    }

    public static boolean isPreMarket(DateTime currentTime) {
        return !isTradingHours(currentTime) && !currentTime.isWeekend() && currentTime.getHour() >= 4 && currentTime.getHour() < 10;
    }

    public static boolean isPostMarket(DateTime currentTime) {
        return !isTradingHours(currentTime) && !currentTime.isWeekend() && currentTime.getHour() >= 16 && currentTime.getHour() < 20;
    }

    public static boolean isPriceActive(boolean extendedHours, DateTime currentTime) {
        if (extendedHours) return isTradingHours(currentTime) || isPreMarket(currentTime) || isPostMarket(currentTime);
        return isTradingHours(currentTime);
    }


    public static boolean shouldClose(DateTime dateTime, TimeFrame timeFrame) {
        return switch (timeFrame) {
            case ONE_SECOND -> true;
            case FIVE_SECONDS -> dateTime.getSecond() % 5 == 0;
            case THIRTY_SECONDS -> dateTime.getSecond() % 30 == 0;
            case ONE_MINUTE -> dateTime.getSecond() == 0;
            case FIVE_MINUTES -> dateTime.getMinute() % 5 == 0 && dateTime.getSecond() == 0;
            case FIFTEEN_MINUTES -> dateTime.getMinute() % 15 == 0 && dateTime.getSecond() == 0;
            case THIRTY_MINUTES -> dateTime.getMinute() % 30 == 0 && dateTime.getSecond() == 0;
            case ONE_HOUR -> dateTime.getHour() == 0 && dateTime.getMinute() == 0 && dateTime.getSecond() == 0;
            case TWO_HOUR -> dateTime.getHour() % 2 == 0 && dateTime.getMinute() == 0 && dateTime.getSecond() == 0;
            case FOUR_HOUR -> dateTime.getHour() % 4 == 0 && dateTime.getMinute() == 0 && dateTime.getSecond() == 0;
            case ONE_DAY -> dateTime.getHour() % 8 == 0 && dateTime.getMinute() == 0 && dateTime.getSecond() == 0;
        };
    }

}
