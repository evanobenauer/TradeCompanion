package com.ejo.tradecompanion.indicator;

import com.ejo.glowlib.time.DateTime;
import com.ejo.stockdownloader.data.Stock;

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

        //TODO: The way this is calculated causes the EMA to start as the SMA whenever the previous candle is NULL
        // lastCandleTime will cause this every time a day starts
        // modify the lastCandleTime to return the last time BEFORE a break. Maybe use a while loop
        DateTime lastCandleTime = new DateTime(dateTime.getYearInt(),dateTime.getMonthInt(),dateTime.getDayInt(),dateTime.getHourInt(),dateTime.getMinuteInt(),dateTime.getSecondInt() - getStock().getTimeFrame().getSeconds());
        double prevOpenEMA;
        double prevCloseEMA;
        try {
            prevOpenEMA = Float.parseFloat(getHistoricalData().get(lastCandleTime.getDateTimeID())[0]);
            prevCloseEMA = Float.parseFloat(getHistoricalData().get(lastCandleTime.getDateTimeID())[1]);
        } catch (NullPointerException e) {
            prevOpenEMA = equivalentSMA.calculateData(dateTime)[0];
            prevCloseEMA = equivalentSMA.calculateData(dateTime)[1];
        }
        double openEMA = open == -1 ? prevOpenEMA : open * weight + prevOpenEMA * (1-weight);
        double closeEMA = close == -1 ? prevCloseEMA : close * weight + prevCloseEMA * (1-weight);

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
