package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.HistoricalDataContainer;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.util.StockUtil;

import java.util.ArrayList;

public class IndicatorMACD extends Indicator {

    private final boolean sma;

    private final IndicatorMA ma1;
    private final IndicatorMA ma2;

    private final HistoricalDataContainer MACD;
    private final IndicatorMA indicator;

    public IndicatorMACD(Stock stock, int lowPeriod, int highPeriod, boolean sma) {
        super(stock, false);
        this.sma = sma;
        if (sma) {
            this.ma1 = new IndicatorSMA(stock, lowPeriod);
            this.ma2 = new IndicatorSMA(stock, highPeriod);
            this.indicator = new IndicatorSMA(stock,9);
        } else {
            this.ma1 = new IndicatorEMA(stock, lowPeriod);
            this.ma2 = new IndicatorEMA(stock, highPeriod);
            this.indicator = new IndicatorEMA(stock,9);
        }


        this.MACD = new IndicatorMA(stock,false,0,ColorE.NULL) {
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
        float valueIndicator = sma ? calculateSMA(dateTime,this.MACD,9)[1] : calculateEMA(dateTime,indicator,this.MACD,9)[1];

        float[] result = new float[]{valueCloseMACD,valueIndicator};
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
        return "MACD" + getMA1().getPeriod() + "-" + getMA2().getPeriod() + (getStock().isExtendedHours() ? "-EH" : "");
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

    private float[] calculateEMA(DateTime dateTime, IndicatorMA ema, HistoricalDataContainer historicalData, int period) {
        float[] data = historicalData.getData(dateTime);
        float open = data[0];
        float close = data[1];

        double weight = (double) 2 / (period + 1);

        int i = 1;
        DateTime lastCandleTime = dateTime.getAdded( - getStock().getTimeFrame().getSeconds() * i);
        while (!StockUtil.isPriceActive(getStock().isExtendedHours(), lastCandleTime)) {
            i++;
            lastCandleTime = dateTime.getAdded( - getStock().getTimeFrame().getSeconds() * i);
        }

        double prevOpenEMA;
        double prevCloseEMA;
        float[] prevEMA = ema.getData(lastCandleTime);
        prevOpenEMA = prevEMA[0];
        prevCloseEMA = prevEMA[1];
        if (prevCloseEMA == -1 || Double.isNaN(prevCloseEMA)) { //If the previous EMA does not exist, set this value to the current SMA
            prevOpenEMA = calculateSMA(dateTime,historicalData,period)[0];
            prevCloseEMA = calculateSMA(dateTime,historicalData,period)[1];
        }

        float openEMA = (float) MathE.roundDouble(open == -1 ? prevOpenEMA : open * weight + prevOpenEMA * (1 - weight), 4);
        float closeEMA = (float) MathE.roundDouble(close == -1 ? prevCloseEMA : close * weight + prevCloseEMA * (1 - weight), 4);

        return new float[]{openEMA, closeEMA};
    }

    private float[] calculateSMA(DateTime dateTime, HistoricalDataContainer ma, int period) {
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

            float[] data = ma.getData(nextDate);
            float open = data[0];
            float close = data[1];
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

}