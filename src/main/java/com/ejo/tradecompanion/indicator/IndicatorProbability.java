package com.ejo.tradecompanion.indicator;

import com.ejo.glowlib.time.DateTime;
import com.ejo.stockdownloader.data.Stock;

import java.util.HashMap;

public class IndicatorProbability extends Indicator {

    public IndicatorProbability(Stock stock) {
        super(stock, false);
    }

    @Override
    public float[] calculateData(DateTime candleTime) {
        //TODO: Put probability shit in here
        return new float[0];
    }

    @Override
    public String getDefaultFileName() {
        return null;
    }

    @Override
    public HashMap<Long, float[]> loadHistoricalData() {
        System.out.println("No Data to Load");
        return new HashMap<>();
    }

    @Override
    public boolean saveHistoricalData() {
        System.out.println("No Data to Save");
        return false;
    }
}
