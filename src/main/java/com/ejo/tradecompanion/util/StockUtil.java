package com.ejo.tradecompanion.util;

import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class StockUtil {

    public static final Container<Integer> SECOND_ADJUST = new Container<>(0);

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

    public static DateTime getAdjustedCurrentTime() {
        DateTime ct = DateTime.getCurrentDateTime();
        return new DateTime(ct.getYear(), ct.getMonth(), ct.getDay(), ct.getHour(), ct.getMinute(), ct.getSecond() + SECOND_ADJUST.get());
    }

}
