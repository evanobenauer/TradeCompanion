package com.ejo.tradecompanion.indicator;

import com.ejo.glowlib.file.CSVManager;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;

import java.util.HashMap;

public abstract class Indicator {

    //Stock
    private final Stock stock;

    //Historical Data HashMap
    private HashMap<Long, String[]> dataHash = new HashMap<>();

    //Live Scrape Data Variables
    private final StopWatch updateTimer = new StopWatch();
    private float open;
    private float close;

    //Calculation Variables
    private final Container<Double> calculationPercent = new Container<>(0d);
    protected boolean calculationActive = false;

    public Indicator(Stock stock, boolean loadOnInstantiation) {
        this.stock = stock;
        if (loadOnInstantiation) loadHistoricalData();
    }

    public Indicator(Stock stock) {
        this(stock,true);
    }

    /**
     * Scrapes live indicator data from a website depending on the chosen data source
     */
    public abstract void updateScrapeData(); //TODO: make sure to save the data to the hash on candle close

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
    public abstract void calculateData(DateTime candleTime);


    /**
     * Calculates indicator data from a range of stock candle dates. The data will be reflective of whether the stock is using extended hours or not
     * @param startCandleTime
     * @param endCandleTime
     */
    public void calculateData(DateTime startCandleTime, DateTime endCandleTime) {
        this.calculationActive = true;
        getCalculationPercent().set(0d);

        if (startCandleTime.getDateTimeID() == endCandleTime.getDateTimeID()) {
            calculateData(startCandleTime);
            return;
        }
        if (endCandleTime.getDateTimeID() < startCandleTime.getDateTimeID()) return;

        DateTime currentDateTime = new DateTime(startCandleTime.getDateTimeID());

        int loopCount = 0;
        while (currentDateTime.getDateTimeID() < endCandleTime.getDateTimeID()) {
            currentDateTime = new DateTime(startCandleTime.getYearInt(),startCandleTime.getMonthInt(),startCandleTime.getDayInt(),startCandleTime.getHourInt(),startCandleTime.getMinuteInt(),startCandleTime.getSecondInt() + loopCount * getStock().getTimeFrame().getSeconds());

            if (!StockUtil.isPriceActive(getStock().isExtendedHours(),currentDateTime)) {
                loopCount++;
                continue;
            }

            getCalculationPercent().set(0d); //TODO: set the calculation percent properly

            calculateData(currentDateTime);
            loopCount++;
        }

        getCalculationPercent().set(1d);
        this.calculationActive = false;
    }

    /**
     * Calculates indicator data from all available stock data dates. The data will be reflective of whether the stock is using extended hours or not
     */
    public void calculateData() {
        calculateData(new DateTime(2000,1,3),StockUtil.getAdjustedCurrentTime());
    }


    /**
     * Loads all historical indicator data from the specified file as a hashmap with datetimeID as the key
     * @param filePath
     * @param fileName
     * @return
     */
    public HashMap<Long, String[]> loadHistoricalData(String filePath, String fileName) {
        try {
            HashMap<String, String[]> rawMap = CSVManager.getHMDataFromCSV(filePath, fileName);

            HashMap<Long, String[]> convertedMap = new HashMap<>();
            for (String key : rawMap.keySet()) {
                if (getStock().isExtendedHours()) {
                    convertedMap.put(Long.parseLong(key), rawMap.get(key));
                } else if (StockUtil.isTradingHours(new DateTime(Long.parseLong(key)))) {
                    convertedMap.put(Long.parseLong(key), rawMap.get(key));
                }
            }
            return this.dataHash = convertedMap;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * Have this specify the default path when overridden like in the Stock class
     * @return
     */
    public abstract HashMap<Long, String[]> loadHistoricalData();


    /**
     * Saves all historical data to a specified path with the key as the datetimeID in the first column
     * @param filePath
     * @param fileName
     * @return
     */
    public boolean saveHistoricalData(String filePath, String fileName) {
        return CSVManager.saveAsCSV(getHistoricalData(), filePath, fileName);
    }

    /**
     * Have this specify the default path when overridden like in the Stock class
     * @return
     */
    public abstract boolean saveHistoricalData();


    public float getOpenValue() {
        return open;
    }

    public float getOpenValue(DateTime dateTime) {
        try {
            if (dateTime == null || dateTime.equals(getStock().getOpenTime())) {
                return getOpenValue();
            } else {
                return Float.parseFloat(getHistoricalData().get(dateTime.getDateTimeID())[0]);
            }
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public float getCloseValue() {
        return close;
    }

    public float getCloseValue(DateTime dateTime) {
        try {
            if (dateTime == null || dateTime.equals(getStock().getOpenTime())) {
                return getCloseValue();
            } else {
                return Float.parseFloat(getHistoricalData().get(dateTime.getDateTimeID())[1]);
            }
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public Container<Double> getCalculationPercent() {
        return calculationPercent;
    }

    public boolean isCalculationActive() {
        return calculationActive;
    }


    public HashMap<Long, String[]> getHistoricalData() {
        return dataHash;
    }

    public Stock getStock() {
        return stock;
    }


    //TODO: make data source options
    enum DataSource {
        SOURCE1
    }
}
