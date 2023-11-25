package com.ejo.tradecompanion.indicator;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;

public class IndicatorEMA extends Indicator {

    private final IndicatorSMA equivalentSMA;

    private final int period;

    private DateTime currentCalculationDate;

    public IndicatorEMA(Stock stock, boolean loadOnInstantiation, int period) {
        super(stock, loadOnInstantiation);
        this.equivalentSMA = new IndicatorSMA(getStock(),false,period);
        this.period = period;
    }

    public IndicatorEMA(Stock stock, int period) {
        this(stock,true,period);
    }

    @Override
    public void updateScrapeData() {

    }

    @Override
    public float[] calculateData(DateTime dateTime) {
        float open = getStock().getOpen(dateTime);
        float close = getStock().getClose(dateTime);

        double weight = (double) 2 / (getPeriod() + 1);

        int i = 1;
        DateTime lastCandleTime = new DateTime(dateTime.getYear(),dateTime.getMonth(),dateTime.getDay(),dateTime.getHour(),dateTime.getMinute(),dateTime.getSecond() - getStock().getTimeFrame().getSeconds() * i);
        while (!StockUtil.isPriceActive(getStock().isExtendedHours(),lastCandleTime)) {
            i++;
            lastCandleTime = new DateTime(lastCandleTime.getYear(),lastCandleTime.getMonth(),lastCandleTime.getDay(),lastCandleTime.getHour(),lastCandleTime.getMinute(),lastCandleTime.getSecond() - getStock().getTimeFrame().getSeconds() * i);;
        }

        double prevOpenEMA;
        double prevCloseEMA;
        try {
            prevOpenEMA = Float.parseFloat(getHistoricalData().get(lastCandleTime.getDateTimeID())[0]);
            prevCloseEMA = Float.parseFloat(getHistoricalData().get(lastCandleTime.getDateTimeID())[1]);
            if (Double.isNaN(prevOpenEMA)) { //If the previous EMA does not exist, set this value to the current SMA
                prevOpenEMA = equivalentSMA.calculateData(dateTime)[0];
                prevCloseEMA = equivalentSMA.calculateData(dateTime)[1];
            }
        } catch (NullPointerException | NumberFormatException e) {
            prevOpenEMA = equivalentSMA.calculateData(dateTime)[0];
            prevCloseEMA = equivalentSMA.calculateData(dateTime)[1];
        }
        double openEMA = MathE.roundDouble(open == -1 ? prevOpenEMA : open * weight + prevOpenEMA * (1-weight),4);
        double closeEMA = MathE.roundDouble(close == -1 ? prevCloseEMA : close * weight + prevCloseEMA * (1-weight),4);
        getHistoricalData().put(dateTime.getDateTimeID(), new String[]{String.valueOf(openEMA) ,String.valueOf(closeEMA)});
        this.currentCalculationDate = dateTime;

        return new float[]{(float) openEMA, (float) closeEMA};
    }

    @Override
    public String getDefaultFileName() {
        return "EMA" + getPeriod() + "-" + getStockLabel() + (getStock().isExtendedHours() ? "-EH" : "");
    }

    public DateTime getCurrentCalculationDate() {
        return currentCalculationDate;
    }

    public int getPeriod() {
        return period;
    }
}
