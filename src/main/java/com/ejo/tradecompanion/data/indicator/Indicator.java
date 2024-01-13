package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowlib.util.TimeUtil;
import com.ejo.tradecompanion.data.HistoricalDataContainer;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.util.StockUtil;

public abstract class Indicator extends HistoricalDataContainer {

    //Stock
    private final Stock stock;

    //Live Scrape Data Variables
    private final StopWatch updateTimer = new StopWatch();


    public Indicator(Stock stock, boolean loadOnInstantiation) {
        this.stock = stock;
        if (loadOnInstantiation) loadHistoricalData();
    }

    /**
     * Scrapes live indicator data from a website depending on the chosen data source
     */
    public void updateScrapeData(){
        //TODO: make sure to save the data to the hash on candle close
        //TODO: Maybe implement this one day. If you choose to, make the method abstract
    }

    /**
     * Scrapes live data every indicated delay
     * @param liveDelayS
     */
    public void updateScrapeData(double liveDelayS) {
        updateTimer.start();
        if (updateTimer.hasTimePassedS(liveDelayS)) {
            updateTimer.restart();
            updateScrapeData();
        }
    }

    /**
     * Calculates all indicator data depending on the stock values/historical values
     */
    public abstract float[] calculateData(DateTime dateTime);


    /**
     * Calculates indicator data from a range of stock candle dates. The data will be reflective of whether the stock is using extended hours or not
     *
     * @param startCandleTime
     * @param endCandleTime
     */
    public void calculateData(DateTime startCandleTime, DateTime endCandleTime) {
        getProgressContainer().set(0d);
        this.progressActive = true;

        if (endCandleTime.getDateTimeID() < startCandleTime.getDateTimeID()) return;
        if (startCandleTime.getDateTimeID() == endCandleTime.getDateTimeID()) {
            calculateData(startCandleTime);
            return;
        }

        DateTime currentDateTime = new DateTime(startCandleTime.getDateTimeID());

        int loopCount = 0;
        while (currentDateTime.getDateTimeID() < endCandleTime.getDateTimeID()) {
            currentDateTime = startCandleTime.getAdded(loopCount * getStock().getTimeFrame().getSeconds());

            if (!StockUtil.isPriceActive(getStock().isExtendedHours(), currentDateTime)) {
                loopCount++;
                continue;
            }

            getProgressContainer().set(TimeUtil.getDateTimePercent(startCandleTime,currentDateTime,endCandleTime));

            calculateData(currentDateTime);
            loopCount++;
        }

        getProgressContainer().set(1d);
        this.progressActive = false;
    }

    /**
     * Calculates indicator data from all available stock data dates. The data will be reflective of whether the stock is using extended hours or not
     */
    public void calculateData() {
        calculateData(new DateTime(2000, 1, 3), TimeUtil.getAdjustedCurrentTime());
    }


    @Override
    public String getDefaultFilePath() {
        return "stock_data/indicator_data";
    }


    public Stock getStock() {
        return stock;
    }

    protected String getStockLabel() {
        return getStock().getTicker() + "_" + getStock().getTimeFrame().getTag();
    }

}
