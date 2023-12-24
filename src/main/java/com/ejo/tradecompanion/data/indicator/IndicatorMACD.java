package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.Stock;

import java.util.HashMap;

public class IndicatorMACD extends Indicator {

    //TODO: Add the option to make these SMA/EMA maybe? idk
    private final IndicatorMA ma1;
    private final IndicatorMA ma2;

    public IndicatorMACD(Stock stock, int lowPeriod, int highPeriod) {
        super(stock, false);
        ma1 = new IndicatorEMA(stock,lowPeriod);
        ma2 = new IndicatorEMA(stock,highPeriod);
    }

    @Override
    public float[] calculateData(DateTime dateTime) {
        ma1.calculateData(dateTime);
        ma2.calculateData(dateTime);
        float[] ma1Data = ma1.getData(dateTime);
        float[] ma2Data = ma2.getData(dateTime);
        return new float[]{ma1Data[0],ma1Data[1],ma2Data[0],ma2Data[1]};
    }

    @Override
    public float[] getData(DateTime dateTime) {
        float[] ma1Data = getMA1().getData(dateTime);
        float[] ma2Data = getMA2().getData(dateTime);
        return new float[]{ma1Data[0],ma1Data[1],ma2Data[0],ma2Data[1]};
    }

    @Override
    public String getDefaultFileName() {
        return "MACD" + getMA1().getPeriod() + "-" + getMA2().getPeriod();
    }

    @Override
    public String toString() {
        return "MACD";
    }

    public IndicatorMA getMA1() {
        return ma1;
    }

    public IndicatorMA getMA2() {
        return ma2;
    }


    public HashMap<Long, float[]> loadHistoricalData(String filePath, String fileName) {
        getProgressContainer().set(0d);
        this.progressActive = true;
        getMA1().loadHistoricalData();
        getProgressContainer().set(.5d);
        getMA2().loadHistoricalData();
        getProgressContainer().set(1d);
        this.progressActive = false;
        return new HashMap<>();
    }

    public boolean saveHistoricalData(String filePath, String fileName) {
        getProgressContainer().set(0d);
        this.progressActive = true;
        getMA1().saveHistoricalData();
        getProgressContainer().set(.5d);
        getMA2().saveHistoricalData();
        getProgressContainer().set(1d);
        this.progressActive = false;
        return true;
    }


    public void applyLoadHistoricalData(String filePath, String fileName) {
        getProgressContainer().set(0d);
        this.progressActive = true;
        getMA1().applyLoadHistoricalData();
        getProgressContainer().set(.5d);
        getMA2().applyLoadHistoricalData();
        getProgressContainer().set(1d);
        this.progressActive = false;
    }


    public boolean applySaveHistoricalData(String filePath, String fileName) {
        getProgressContainer().set(0d);
        this.progressActive = true;
        getMA1().applySaveHistoricalData();
        getProgressContainer().set(.5d);
        getMA2().applySaveHistoricalData();
        getProgressContainer().set(1d);
        this.progressActive = false;
        return true;
    }

}
