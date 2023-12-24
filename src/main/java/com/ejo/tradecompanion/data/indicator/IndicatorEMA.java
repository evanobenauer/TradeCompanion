package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.util.StockUtil;

public class IndicatorEMA extends IndicatorMA {

    private final IndicatorSMA equivalentSMA;

    private DateTime currentCalculationDate;

    public IndicatorEMA(Stock stock, int period) {
        super(stock,false,period, ColorE.YELLOW);
        this.equivalentSMA = new IndicatorSMA(getStock(), period);
    }

    @Override
    public float[] calculateData(DateTime dateTime) {
        float[] data = getStock().getData(dateTime);
        float open = data[0];
        float close = data[1];

        double weight = (double) 2 / (getPeriod() + 1);

        int i = 1;
        DateTime lastCandleTime = dateTime.getAdded( - getStock().getTimeFrame().getSeconds() * i);
        while (!StockUtil.isPriceActive(getStock().isExtendedHours(), lastCandleTime)) {
            i++;
            lastCandleTime = dateTime.getAdded( - getStock().getTimeFrame().getSeconds() * i);
        }

        double prevOpenEMA;
        double prevCloseEMA;
        float[] prevEMA = getData(lastCandleTime);
        prevOpenEMA = prevEMA[0];
        prevCloseEMA = prevEMA[1];
        if (prevOpenEMA == -1 || Double.isNaN(prevOpenEMA)) { //If the previous EMA does not exist, set this value to the current SMA
            prevOpenEMA = equivalentSMA.calculateData(dateTime)[0];
            prevCloseEMA = equivalentSMA.calculateData(dateTime)[1];
        }

        float openEMA = (float) MathE.roundDouble(open == -1 ? prevOpenEMA : open * weight + prevOpenEMA * (1 - weight), 4);
        float closeEMA = (float) MathE.roundDouble(close == -1 ? prevCloseEMA : close * weight + prevCloseEMA * (1 - weight), 4);
        float[] result = new float[]{openEMA, closeEMA};
        getHistoricalData().put(dateTime.getDateTimeID(), result);
        this.currentCalculationDate = dateTime;

        return result;
    }

    @Override
    public String getDefaultFileName() {
        return "EMA" + getPeriod() + "-" + getStockLabel() + (getStock().isExtendedHours() ? "-EH" : "");
    }

    public DateTime getCurrentCalculationDate() {
        return currentCalculationDate;
    }

    @Override
    public String toString() {
        return "EMA" + getPeriod();
    }
}
