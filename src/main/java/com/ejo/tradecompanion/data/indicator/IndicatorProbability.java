package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.util.ProbabilityUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class IndicatorProbability extends Indicator {

    public IndicatorProbability(Stock stock) {
        super(stock, false);
    }

    @Override
    public float[] calculateData(DateTime candleTime) {
        System.out.println("---");
        System.out.println("Starting Calculation for candle: " + candleTime);
        float precision = .03f; //Maybe try .02f?
        int lookBackAmount = 4;
        int lookForwardAmount = 5;
        boolean priceScale = false;
        boolean ignoreWicks = true;
        boolean includeAfterHours = false;

        ArrayList<ArrayList<Long>> similarResultsList = new ArrayList<>();
        Container<float[]> results = new Container<>();

        similarResultsList.add(ProbabilityUtil.getSimilarCandleIDs(getStock(), candleTime, precision, priceScale, ignoreWicks, includeAfterHours, lookForwardAmount, results));
        System.out.println("---");
        System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in " + lookForwardAmount + " Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
        System.out.println("Run Time: " + results.get()[6] + "s");

        for (int l = 1; l <= lookBackAmount; l++) {
            similarResultsList.add(ProbabilityUtil.filterSimilarCandlesFromPrevious(getStock(), candleTime, precision, priceScale, ignoreWicks, includeAfterHours, similarResultsList.get(l - 1), l, lookForwardAmount, results));
            //Set the hashmap data with the results here
            System.out.println("---");
            System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in " + lookForwardAmount + " Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
            System.out.println("Run Time: " + results.get()[6] + "s");
        }
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
}
