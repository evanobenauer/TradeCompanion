package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.HistoricalDataContainer;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.util.StockUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class IndicatorMACD extends Indicator {

    private final boolean sma;

    private final IndicatorMA ma1;
    private final IndicatorMA ma2;

    private final HistoricalDataContainer MACD;
    private final IndicatorMA signal;

    public IndicatorMACD(Stock stock, int lowPeriod, int highPeriod, int signalPeriod, boolean sma) {
        super(stock, false);
        this.sma = sma;
        if (sma) {
            this.ma1 = new IndicatorSMA(stock, lowPeriod,null,null,0);
            this.ma2 = new IndicatorSMA(stock, highPeriod,null,null,0);
            this.signal = new IndicatorSMA(stock,signalPeriod,null,null,0);
        } else {
            this.ma1 = new IndicatorEMA(stock, lowPeriod,null,null,0);
            this.ma2 = new IndicatorEMA(stock, highPeriod,null,null,0);
            this.signal = new IndicatorEMA(stock,signalPeriod,null,null,0);
        }


        this.MACD = new IndicatorMA(stock,false,0, IndicatorMA.Type.CLOSE,null,0) {
            @Override
            public String getDefaultFileName() {
                return null;
            }
        };
    }

    @Override
    public float[] calculateData(DateTime dateTime) {
        float valueOpenMACD = 0; //DNE
        float valueCloseMACD = getMA1().calculateData(dateTime)[1] - getMA2().calculateData(dateTime)[1];
        this.MACD.getHistoricalData().put(dateTime.getDateTimeID(),new float[]{valueOpenMACD,valueCloseMACD});

        float valueSignal = sma ? calculateSMA(dateTime,this.MACD,this.signal.getPeriod())[1] : calculateEMA(dateTime,this.MACD,this.signal.getPeriod())[1];

        float[] result = new float[]{valueCloseMACD,valueSignal};
        getHistoricalData().put(dateTime.getDateTimeID(), result);
        return result;
    }

    @Override
    public float[] getData(DateTime dateTime) {
        float[] rawData = getHistoricalData().get(dateTime.getDateTimeID());
        if (rawData == null) return new float[]{-1,-1};
        return rawData;
    }

    @Override
    public String getDefaultFileName() {
        return "MACD" + getMA1().getPeriod() + "-" + getMA2().getPeriod() + "-" + getStockLabel() + (getStock().isExtendedHours() ? "-EH" : "");
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

    private float[] calculateEMA(DateTime dateTime, HistoricalDataContainer data, int period) {
        float macdClose = data.getData(dateTime)[1];

        double weight = (double) 2 / (period + 1);

        int i = 1;
        DateTime lastCandleTime = dateTime.getAdded( - getStock().getTimeFrame().getSeconds() * i);
        while (!StockUtil.isPriceActive(getStock().isExtendedHours(), lastCandleTime)) {
            i++;
            lastCandleTime = dateTime.getAdded( - getStock().getTimeFrame().getSeconds() * i);
        }

        float prevEMA = getData(lastCandleTime)[1];
        if (prevEMA == -1 || Double.isNaN(prevEMA)) { //If the previous EMA does not exist, set this value to the current SMA
            prevEMA = calculateSMA(dateTime,data,period)[1];
        }

        float closeEMA = (float) MathE.roundDouble(macdClose == -1 ? prevEMA : macdClose * weight + prevEMA * (1 - weight), 4);
        float[] result = new float[]{0, closeEMA};
        this.signal.getHistoricalData().put(dateTime.getDateTimeID(), result);

        return result;
    }

    private float[] calculateSMA(DateTime dateTime, HistoricalDataContainer data, int period) {
        if (!StockUtil.isPriceActive(getStock().isExtendedHours(),dateTime)) return new float[]{-1,-1};
        //SMA CALCULATION
        ArrayList<Float> openAvgList = new ArrayList<>();
        ArrayList<Float> closeAvgList = new ArrayList<>();

        //Adds the period of candles behind our goal candle together to the avg list
        int candleCount = 0;
        int loopCount = 0;
        while (candleCount < period) {
            DateTime nextDate = dateTime.getAdded(-loopCount * getStock().getTimeFrame().getSeconds());
            if (!StockUtil.isPriceActive(getStock().isExtendedHours(),nextDate)) {
                loopCount++;
                continue;
            }

            float[] values = data.getData(nextDate);
            float open = values[0];
            float close = values[1];
            if (open != -1) openAvgList.add(open);
            if (close != -1) closeAvgList.add(close);

            candleCount++;
            loopCount++;
        }

        float openAvg = (float) MathE.roundDouble(calculateAverage(openAvgList),4);
        float closeAvg = (float) MathE.roundDouble(calculateAverage(closeAvgList),4);
        return new float[]{openAvg ,closeAvg};
    }

    public static <T extends Number> double calculateAverage(ArrayList<T> values) {
        double avg = 0;
        for (T val : values) {
            avg += val.doubleValue();
        }
        avg /= values.size();
        return avg;
    }

    @Override
    public HashMap<Long, float[]> loadHistoricalData(String filePath, String fileName) {
        this.progressActive = true;
        this.getProgressContainer().set(0d);
        getMA1().loadHistoricalData();
        getMA2().loadHistoricalData();
        return super.loadHistoricalData(filePath, fileName);
    }

    @Override
    public boolean saveHistoricalData(String filePath, String fileName) {
        this.progressActive = true;
        this.getProgressContainer().set(0d);
        getMA1().saveHistoricalData();
        getMA2().saveHistoricalData();
        return super.saveHistoricalData(filePath, fileName);
    }

    @Override
    public void applyLoadHistoricalData(String filePath, String fileName) {
        this.progressActive = true;
        this.getProgressContainer().set(0d);
        getMA1().applyLoadHistoricalData();
        getMA2().applyLoadHistoricalData();
        super.applyLoadHistoricalData(filePath, fileName);
    }

    @Override
    public boolean applySaveHistoricalData(String filePath, String fileName) {
        this.progressActive = true;
        this.getProgressContainer().set(0d);
        getMA1().applySaveHistoricalData();
        getMA2().applySaveHistoricalData();
        return super.applySaveHistoricalData(filePath, fileName);
    }
}