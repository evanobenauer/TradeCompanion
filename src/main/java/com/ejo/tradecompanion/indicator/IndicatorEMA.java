package com.ejo.tradecompanion.indicator;

import com.ejo.glowlib.time.DateTime;
import com.ejo.stockdownloader.data.Stock;

public class IndicatorEMA extends Indicator {

    private final int period;

    private DateTime currentCalculationDate;

    public IndicatorEMA(Stock stock, boolean loadOnInstantiation, int period) {
        super(stock, loadOnInstantiation);
        this.period = period;
    }

    public IndicatorEMA(Stock stock, int period) {
        this(stock,true,period);
    }

    @Override
    public void updateScrapeData() {

    }

    @Override
    public float[] calculateData(DateTime candleTime) {
        return new float[0];
    }

    @Override
    public String getDefaultFileName() {
        return "EMA" + getPeriod() + "-" + STOCK_LABEL + (getStock().isExtendedHours() ? "-EH" : "");
    }

    public DateTime getCurrentCalculationDate() {
        return currentCalculationDate;
    }

    public int getPeriod() {
        return period;
    }
}
