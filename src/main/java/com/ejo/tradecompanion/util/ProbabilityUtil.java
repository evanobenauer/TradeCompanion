package com.ejo.tradecompanion.util;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProbabilityUtil {

    //TODO: maybe add time of day & weekday probability
    //TODO: Find a way to throw out garbage data from 2000-2003. It messes stuff up. Maybe just exclude it?
    //TODO: Add previous calculation section back to the last 3 so they can be compared especially when working with live data

    //TODO: Add an option to look back only a certain timeframe to make pattern recognition more reflective of current market trends

    public static ArrayList<Long> getSimilarCandleIDs(Stock stock, DateTime candleTime, float marginPrice, boolean doPriceScaling, boolean ignoreWicks, boolean includeAfterHours, int lookForwardAmount, Container<float[]> resultsContainer) {
        DateTime startTime = DateTime.getCurrentDateTime();

        ArrayList<Long> similarCandleList = new ArrayList<>();
        int similarCandleCount = 0;

        //Next Candles
        int nextGreen = 0;
        int nextRed = 0;

        //Look Forward Results
        float lookForwardAvgChange = 0;
        int lookForwardGreen = 0;
        int lookForwardRed = 0;

        //Define Shorthand Variables
        int timeOffset = stock.getTimeFrame().getSeconds();

        //Current Data
        float[] mainData = stock.getData(candleTime);
        long mainID = candleTime.getDateTimeID();


        for (Map.Entry<Long, float[]> rawData : stock.getHistoricalData().entrySet()) { //Loops through all stock data; This may throw a concurrent modification exception
            float[] testData = rawData.getValue();
            long testID = rawData.getKey();

            DateTime testTime = new DateTime(testID);

            if (testID == mainID) continue;
            if (!includeAfterHours && !StockUtil.isTradingHours(testTime)) continue;
            if (testID < 20040000000000L) continue;//TEMPORARY REMOVE EARLY DATA. IT SUCKS

            if (areCandlesSimilar(mainData, testData, marginPrice, doPriceScaling, ignoreWicks)) {
                float[] nextData = stock.getData(testTime.getAdded(timeOffset));
                float[] lookForwardData = stock.getData(testTime.getAdded(timeOffset * lookForwardAmount));
                if (nextData[0] == -1 || lookForwardData[0] == -1) continue;

                //Next Candle Data
                if (nextData[1] > nextData[0]) nextGreen++;
                if (nextData[1] < nextData[0]) nextRed++;

                //Forward Prediction
                float testClose = testData[1];
                float lookForwardClose = lookForwardData[1];
                lookForwardAvgChange += lookForwardClose - testClose;
                if (lookForwardClose > testClose) lookForwardGreen++;
                if (lookForwardClose < testClose) lookForwardRed++;

                similarCandleList.add(testID);
                similarCandleCount++;
            }
        }

        //Calculate Average Close in Three Candles
        if (similarCandleCount == 0) lookForwardAvgChange = 0;
        else lookForwardAvgChange /= similarCandleCount;
        lookForwardAvgChange = (float) MathE.roundDouble(lookForwardAvgChange, 2);

        DateTime endTime = DateTime.getCurrentDateTime();

        resultsContainer.set(new float[]{similarCandleCount, (float) MathE.roundDouble((double) nextGreen / similarCandleCount * 100, 1), (float) MathE.roundDouble((double) nextRed / similarCandleCount * 100, 1), lookForwardAvgChange, (float) MathE.roundDouble((double) lookForwardGreen / similarCandleCount * 100, 1), (float) MathE.roundDouble((double) lookForwardRed / similarCandleCount * 100, 1), (float) (endTime.getCalendar().getTimeInMillis() - startTime.getCalendar().getTimeInMillis()) / 1000});
        return similarCandleList;
    }

    public static ArrayList<Long> filterSimilarCandlesFromPrevious(Stock stock, DateTime candleTime, float marginPrice, boolean doPriceScaling, boolean ignoreWicks, boolean includeAfterHours, ArrayList<Long> similarCandles, int previous, int lookForwardAmount, Container<float[]> resultsContainer) {
        DateTime startTime = DateTime.getCurrentDateTime();

        ArrayList<Long> similarCandleList = new ArrayList<>();
        int similarCandleCount = 0;

        //Next Candles
        int nextGreen = 0;
        int nextRed = 0;

        //Look Forward Results
        float lookForwardAvgChange = 0;
        int lookForwardGreen = 0;
        int lookForwardRed = 0;

        //Define Shorthand Variables
        int timeOffset = stock.getTimeFrame().getSeconds();

        //Previous Candle Data
        float[] prevMainData = stock.getData(candleTime.getAdded(-timeOffset * previous));


        //Checks if the previous candle is similar also. This will filter out all who do not have a similar previous candle
        for (Long id : similarCandles) {
            DateTime similarTime = new DateTime(id);
            if (!includeAfterHours && !StockUtil.isTradingHours(similarTime)) continue;

            //Define Data
            float[] similarData = stock.getData(similarTime);
            float[] prevTestData = stock.getData(similarTime.getAdded(-timeOffset * previous));
            if (prevTestData[0] == -1 || similarData[1] == -1) continue;

            if (areCandlesSimilar(prevMainData, prevTestData, marginPrice, doPriceScaling, ignoreWicks)) {
                float[] nextData = stock.getData(similarTime.getAdded(timeOffset));
                float[] lookForwardData = stock.getData(similarTime.getAdded(timeOffset * lookForwardAmount));
                if (nextData[0] == -1 || lookForwardData[0] == -1) continue;

                //Next Candle Data
                if (nextData[1] > nextData[0]) nextGreen++;
                if (nextData[1] < nextData[0]) nextRed++;

                //Forward Prediction
                float testClose = similarData[1];
                float lookForwardClose = lookForwardData[1];
                lookForwardAvgChange += lookForwardClose - testClose;
                if (lookForwardClose > testClose) lookForwardGreen++;
                if (lookForwardClose < testClose) lookForwardRed++;

                similarCandleList.add(id);
                similarCandleCount++;
            }
        }

        //Calculate Average Close in Three Candles
        if (similarCandleCount == 0) lookForwardAvgChange = 0;
        else lookForwardAvgChange /= similarCandleCount;
        lookForwardAvgChange = (float) MathE.roundDouble(lookForwardAvgChange, 2);

        DateTime endTime = DateTime.getCurrentDateTime();

        resultsContainer.set(new float[]{similarCandleCount, (float) MathE.roundDouble((double) nextGreen / similarCandleCount * 100, 1), (float) MathE.roundDouble((double) nextRed / similarCandleCount * 100, 1), lookForwardAvgChange, (float) MathE.roundDouble((double) lookForwardGreen / similarCandleCount * 100, 1), (float) MathE.roundDouble((double) lookForwardRed / similarCandleCount * 100, 1), (float) (endTime.getCalendar().getTimeInMillis() - startTime.getCalendar().getTimeInMillis()) / 1000});

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

        //This compares the scaled test open price with the main price AND the body size. This may cause no difference
        //boolean bodySize = isWithinMargin(mainOpen,testOpen / pricingScale,marginPrice) && isWithinMargin(getOpenCloseDifference(mainOpen,mainClose),getOpenCloseDifference(testOpen,testClose),marginPrice);

        //This compares the body size
        boolean bodySize = isWithinMargin(getOpenCloseDifference(mainOpen, mainClose), getOpenCloseDifference(testOpen, testClose) / pricingScale, marginPrice);

        if (ignoreWicks) {
            return bodySize;
        } else {
            //TODO: To include wicks, Make make wicks have a much higher margin to avoid megawicks?
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
