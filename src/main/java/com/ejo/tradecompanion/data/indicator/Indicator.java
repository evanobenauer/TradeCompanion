package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.file.FileManager;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.tradecompanion.data.HistoricalDataContainer;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.util.StockUtil;
import com.ejo.tradecompanion.util.TimeUtil;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

public abstract class Indicator extends HistoricalDataContainer {

    //Stock
    private final Stock stock;

    //Live Scrape Data Variables
    private final StopWatch updateTimer = new StopWatch();
    private float open;
    private float close;


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
     *
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
    public abstract float[] calculateData(DateTime candleTime); //TODO: Implement live web scraping


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
        calculateData(new DateTime(2000, 1, 3), StockUtil.getAdjustedCurrentTime());
    }


    @Override
    public String getDefaultFilePath() {
        return "stock_data/indicator_data";
    }

    /**
     * When needing to create multiple value calls, it is smart to assigned getData to an array and call from there as it
     * is much more resource efficient and can speed up a program
     * @param dateTime
     * @return
     */
    public float[] getData(DateTime dateTime) {
        float[] rawData = getHistoricalData().get(dateTime.getDateTimeID());
        if (rawData == null) return new float[]{-1,-1};
        return rawData;
    }

    public float getLiveOpenValue() {
        return open;
    }

    public float getOpenValue(DateTime dateTime) {
        if (dateTime.equals(getStock().getOpenTime())) return calculateData(getStock().getOpenTime())[0];
        return getData(dateTime)[0];
    }

    public float getLiveCloseValue() {
        return close;
    }

    public float getCloseValue(DateTime dateTime) {
        if (dateTime.equals(getStock().getOpenTime())) return calculateData(getStock().getOpenTime())[1];
        return getData(dateTime)[1];
    }


    public Stock getStock() {
        return stock;
    }

    protected String getStockLabel() {
        return getStock().getTicker() + "_" + getStock().getTimeFrame().getTag();
    }

}
