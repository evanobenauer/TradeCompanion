package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.util.ProbabilityUtil;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

public class IndicatorProbability extends Indicator {

    private final float precision;
    private final int patternLookBackAmount;
    private final int predictionForwardAmount;
    private final boolean priceScaling;
    private final boolean ignoreWicks;
    private final boolean includeAfterHours;

    private float previousCalculationMS = 0;

    private boolean calculating = false;

    public IndicatorProbability(Stock stock, float precision, int patternLookBackAmount, int predictionForwardAmount, boolean priceScaling, boolean ignoreWicks, boolean includeAfterHours) {
        super(stock, false);
        this.precision = precision;
        this.patternLookBackAmount = patternLookBackAmount;
        this.predictionForwardAmount = predictionForwardAmount;
        this.priceScaling = priceScaling;
        this.ignoreWicks = ignoreWicks;
        this.includeAfterHours = includeAfterHours;
    }

    @Override
    public float[] calculateData(DateTime dateTime) {
        this.calculating = true;
        this.previousCalculationMS = 0;
        System.out.println("---");
        System.out.println("Starting Calculation for candle: " + dateTime);

        ArrayList<ArrayList<Long>> similarResultsList = new ArrayList<>();
        Container<float[]> results = new Container<>();

        try {
            similarResultsList.add(ProbabilityUtil.getSimilarCandleIDs(getStock(), dateTime, getPrecision(), doPriceScaling(), shouldIgnoreWicks(), shouldIncludeAfterHours(), getPredictionForwardAmount(), results));
            previousCalculationMS += results.get()[6];
            System.out.println("---");
            System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in " + getPredictionForwardAmount() + " Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
            System.out.println("Run Time: " + results.get()[6] + "s");

            for (int l = 1; l <= getPatternLookBackAmount(); l++) {
                similarResultsList.add(ProbabilityUtil.filterSimilarCandlesFromPrevious(getStock(), dateTime, getPrecision(), doPriceScaling(), shouldIgnoreWicks(), shouldIncludeAfterHours(), similarResultsList.get(l - 1), l, getPredictionForwardAmount(), results));
                previousCalculationMS += results.get()[6];
                System.out.println("---");
                System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in " + getPredictionForwardAmount() + " Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
                System.out.println("Run Time: " + results.get()[6] + "s");
                //Possibly add a way to see every look back result
            }
            this.calculating = false;

            //Results Key:
            // 0: Candles
            // 1: +1 Green Probability
            // 2: +1 Red Probability
            // 3: +5 Avg Price Change
            // 4: +5 Rise Probability
            // 5: +5 Fall Probability
            // 6: runtime
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
        getHistoricalData().put(dateTime.getDateTimeID(), results.get());
        return results.get();
    }

    @Override
    public String getDefaultFileName() {
        return null;
    }


    //TODO: Maybe create a save/load section for all previously calculated data to reference?
    @Override
    public HashMap<Long, float[]> loadHistoricalData() {
        System.out.println("Probability Indicator: No Data to Load");
        return new HashMap<>();
    }

    @Override
    public boolean saveHistoricalData() {
        System.out.println("Probability Indicator: No Data to Save");
        return false;
    }

    @Override
    public float[] getData(DateTime dateTime) {
        float[] rawData = getHistoricalData().get(dateTime.getDateTimeID());
        if (rawData == null) return new float[]{-1,-1,-1,-1,-1,-1,-1};
        return rawData;
    }

    @Override
    public String toString() {
        return "Probability";
    }

    public float getPrecision() {
        return precision;
    }

    public int getPatternLookBackAmount() {
        return patternLookBackAmount;
    }

    public int getPredictionForwardAmount() {
        return predictionForwardAmount;
    }

    public boolean doPriceScaling() {
        return priceScaling;
    }

    public boolean shouldIgnoreWicks() {
        return ignoreWicks;
    }

    public boolean shouldIncludeAfterHours() {
        return includeAfterHours;
    }

    public boolean isCalculating() {
        return calculating;
    }

    public float getPrevCalcRuntime() {
        return previousCalculationMS;
    }

}
