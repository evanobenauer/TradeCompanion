package com.ejo.tradecompanion.util;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProbabilityUtil {

    //TODO: maybe add time of day & weekday probability
    //TODO: Find a way to throw out garbage data from 2000-2003. It messes stuff up. Maybe just exclude it?
    //TODO: Add previous calculation section back to the last 3 so they can be compared especially when working with live data


    public static ArrayList<Long> getSimilarCandleIDs(Stock stock, DateTime candleTime, float marginPrice, boolean doPriceScaling, boolean ignoreWicks, boolean includeAfterHours, int lookForwardAmount, Container<float[]> resultsContainer) {
        ArrayList<Long> similarCandleList = new ArrayList<>();

        int similarCandleCount = 0;

        //Next Candles
        int nextGreen = 0;
        int nextRed = 0;

        //Results in 3 Candles
        float avgCloseInThreeCandles = 0;
        int greenCloseInThreeCandles = 0;
        int redCloseInThreeCandles = 0;

        //Current Data
        float[] mainData = stock.getData(candleTime);
        long mainID = candleTime.getDateTimeID();


        //This may throw a concurrent modification exception
        for (Map.Entry<Long, float[]> rawData : stock.getHistoricalData().entrySet()) { //Loops through all stock data
            float[] testData = rawData.getValue();
            long testID = rawData.getKey();

            if (testID == mainID) continue;
            if (!includeAfterHours && !StockUtil.isTradingHours(new DateTime(testID))) continue;
            if (testID < 20040000000000L) continue;//TEMPORARY REMOVE EARLY DATA. IT SUCKS

            if (areCandlesSimilar(mainData, testData, marginPrice, doPriceScaling, ignoreWicks)) {
                similarCandleList.add(testID);

                DateTime thisTime = new DateTime(testID);
                int timeOffset = stock.getTimeFrame().getSeconds();

                //Next Candle Data
                float[] nextData = stock.getData(thisTime.getAdded(timeOffset));
                if (nextData[1] > nextData[0]) nextGreen++;
                if (nextData[1] < nextData[0]) nextRed++;

                //Three Candles Data
                float[] threeCandlesData = stock.getData(thisTime.getAdded(timeOffset * lookForwardAmount));
                float thisClose = stock.getClose(thisTime);
                avgCloseInThreeCandles += threeCandlesData[1] - thisClose;
                if (threeCandlesData[1] > thisClose) greenCloseInThreeCandles++;
                if (threeCandlesData[1] < thisClose) redCloseInThreeCandles++;

                similarCandleCount++;
            }
        }

        //Calculate Average Close in Three Candles
        if (similarCandleCount == 0) avgCloseInThreeCandles = 0;
        else avgCloseInThreeCandles /= similarCandleCount;
        avgCloseInThreeCandles = (float) MathE.roundDouble(avgCloseInThreeCandles, 2);

        resultsContainer.set(new float[]{similarCandleCount, (float) MathE.roundDouble((double) nextGreen / similarCandleCount * 100, 1), (float) MathE.roundDouble((double) nextRed / similarCandleCount * 100, 1), avgCloseInThreeCandles, (float) MathE.roundDouble((double) greenCloseInThreeCandles / similarCandleCount * 100, 1), (float) MathE.roundDouble((double) redCloseInThreeCandles / similarCandleCount * 100, 1)});

        return similarCandleList;
    }

    public static ArrayList<Long> filterSimilarCandlesFromPrevious(Stock stock, DateTime candleTime, float marginPrice, boolean doPriceScaling, boolean ignoreWicks, boolean includeAfterHours, ArrayList<Long> similarCandles, int previous, int lookForwardAmount, Container<float[]> resultsContainer) {
        ArrayList<Long> similarCandleList = new ArrayList<>();
        HashMap<Long, float[]> historicalData = stock.getHistoricalData();

        int similarCandleCount = 0;

        //Next Candles
        int nextGreen = 0;
        int nextRed = 0;

        //Results in 3 Candles
        float avgCloseInThreeCandles = 0;
        int greenCloseInThreeCandles = 0;
        int redCloseInThreeCandles = 0;

        //Previous Candle Data
        DateTime prevCandleTime = candleTime.getAdded(-stock.getTimeFrame().getSeconds() * previous);
        float[] prevMainData = stock.getData(prevCandleTime);


        //Checks if the previous candle is similar also. This will filter out all who do not have a similar previous candle
        for (Long id : similarCandles) {
            if (!includeAfterHours && !StockUtil.isTradingHours(new DateTime(id))) continue;

            DateTime thisTime = new DateTime(id);
            Long prevID = thisTime.getAdded(-stock.getTimeFrame().getSeconds() * previous).getDateTimeID();

            float[] prevTestData = historicalData.get(prevID);
            if (prevTestData == null) continue;

            if (areCandlesSimilar(prevMainData, prevTestData, marginPrice, doPriceScaling, ignoreWicks)) {
                similarCandleList.add(id);

                //Next Candle Data
                int timeOffset = stock.getTimeFrame().getSeconds();
                float[] nextData = stock.getData(thisTime.getAdded(timeOffset));
                if (nextData[1] > nextData[0]) nextGreen++;
                if (nextData[1] < nextData[0]) nextRed++;

                //Three Candles Data
                float[] threeCandlesData = stock.getData(thisTime.getAdded(timeOffset * lookForwardAmount));
                float thisClose = stock.getClose(thisTime);
                avgCloseInThreeCandles += threeCandlesData[1] - thisClose;
                if (threeCandlesData[1] > thisClose) greenCloseInThreeCandles++;
                if (threeCandlesData[1] < thisClose) redCloseInThreeCandles++;

                //More can be added here. Like nextNextNextGreen to look farther in the future based on the built pattern
                // You could also check what the live close price is a few candles ahead to see the average price chance over time

                similarCandleCount++;
            }
        }

        //Calculate Average Close in Three Candles
        if (similarCandleCount == 0) avgCloseInThreeCandles = 0;
        else avgCloseInThreeCandles /= similarCandleCount;
        avgCloseInThreeCandles = (float) MathE.roundDouble(avgCloseInThreeCandles, 2);

        resultsContainer.set(new float[]{similarCandleCount, (float) MathE.roundDouble((double) nextGreen / similarCandleCount * 100, 1), (float) MathE.roundDouble((double) nextRed / similarCandleCount * 100, 1), avgCloseInThreeCandles, (float) MathE.roundDouble((double) greenCloseInThreeCandles / similarCandleCount * 100, 1), (float) MathE.roundDouble((double) redCloseInThreeCandles / similarCandleCount * 100, 1)});

        return similarCandleList;
    }


    //TODO: Testing how candles are similar is the big challenge. Mess around with this more to get a good result
    public static boolean areCandlesSimilar(float[] mainData, float[] testData, float marginPrice, boolean doPriceScaling, boolean ignoreWicks) {
        float mainOpen = mainData[0];
        float mainClose = mainData[1];
        float mainMin = mainData[2];
        float mainMax = mainData[3];
        float mainTopWick = getTopWickSize(mainMax, mainOpen, mainClose);
        float mainBottomWick = getBottomWickSize(mainMin, mainOpen, mainClose);

        float testOpen = testData[0];
        float testClose = testData[1];
        float testMin = testData[2];
        float testMax = testData[3];

        float pricingScale = doPriceScaling ? testOpen / mainOpen : 1;

        //TODO: Maybe use a method that adds all the variation together into one total percent that has to be below a max percent
        //TODO: since the data may cause candles to start at the wrong y pos, maybe add some sort of check for this?

        boolean bodySize = isWithinMargin(mainOpen,testOpen / pricingScale,marginPrice) && isWithinMargin(getOpenCloseDifference(mainOpen,mainClose),getOpenCloseDifference(testOpen,testClose),marginPrice);

        if (ignoreWicks) {
            return bodySize;
        } else {
            //TODO: Make make wicks have a much higher margin to avoid megawicks?
            boolean topWickSize = isWithinMargin(mainTopWick, getTopWickSize(testMax, testOpen, testClose) / pricingScale, marginPrice);
            boolean bottomWickSize = isWithinMargin(mainBottomWick, getBottomWickSize(testMin, testOpen, testClose) / pricingScale, marginPrice);
            return bodySize && topWickSize && bottomWickSize;
        }
    }

    //This is MUCH slower
    public static boolean areCandlesSimilar(Stock stock, DateTime mainTime, DateTime testTime, float marginPrice, boolean doPriceScaling) {
        return areCandlesSimilar(stock.getData(mainTime), stock.getData(testTime), marginPrice, doPriceScaling, false);
    }

    public static boolean isWithinMargin(float mainValue, float testValue, float margin) {
        return (mainValue + margin >= testValue && mainValue - margin <= testValue);
    }

    public static float getTopWickSize(float max, float open, float close) {
        return getOpenCloseDifference(open, close) >= 0 ? max - close : max - open;
    }

    public static float getBottomWickSize(float min, float open, float close) {
        return getOpenCloseDifference(open, close) <= 0 ? close - min : open - min;
    }

    public static float getOpenCloseDifference(float open, float close) {
        return close - open;
    }

}
