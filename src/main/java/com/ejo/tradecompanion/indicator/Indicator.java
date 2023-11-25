package com.ejo.tradecompanion.indicator;

import com.ejo.glowlib.file.CSVManager;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

public abstract class Indicator {

    //File Path
    protected static final String MAIN_PATH = "stock_data/indicator_data";

    //Stock
    private final Stock stock;

    //Historical Data HashMap
    private HashMap<Long, String[]> dataHash = new HashMap<>();

    //Live Scrape Data Variables
    private final StopWatch updateTimer = new StopWatch();
    private float open;
    private float close;

    //Calculation Variables
    private final Container<Double> progressContainer = new Container<>(0d);
    protected boolean progressActive = false;


    public Indicator(Stock stock, boolean loadOnInstantiation) {
        this.stock = stock;
        if (loadOnInstantiation) loadHistoricalData();
    }

    public Indicator(Stock stock) {
        this(stock, true);
    }

    /**
     * Scrapes live indicator data from a website depending on the chosen data source
     */
    public abstract void updateScrapeData(); //TODO: make sure to save the data to the hash on candle close

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
        this.progressActive = true;
        getProgressContainer().set(0d);

        if (endCandleTime.getDateTimeID() < startCandleTime.getDateTimeID()) return;
        if (startCandleTime.getDateTimeID() == endCandleTime.getDateTimeID()) {
            calculateData(startCandleTime);
            return;
        }

        DateTime currentDateTime = new DateTime(startCandleTime.getDateTimeID());

        int loopCount = 0;
        while (currentDateTime.getDateTimeID() < endCandleTime.getDateTimeID()) {
            currentDateTime = new DateTime(startCandleTime.getYear(), startCandleTime.getMonth(), startCandleTime.getDay(), startCandleTime.getHour(), startCandleTime.getMinute(), startCandleTime.getSecond() + loopCount * getStock().getTimeFrame().getSeconds());

            if (!StockUtil.isPriceActive(getStock().isExtendedHours(), currentDateTime)) {
                loopCount++;
                continue;
            }

            getProgressContainer().set(getDateTimePercent(startCandleTime,currentDateTime,endCandleTime));

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


    /**
     * Loads all historical indicator data from the specified file as a hashmap with datetimeID as the key
     *
     * @param filePath
     * @param fileName
     * @return
     */
    public HashMap<Long, String[]> loadHistoricalData(String filePath, String fileName) {
        this.progressActive = true;
        try {
            File file = new File(filePath + (fileName.equals("") ? "" : "/") + fileName.replace(".csv", "") + ".csv");
            HashMap<Long, String[]> rawMap = new HashMap<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                long fileSize = Files.lines(file.toPath()).count();
                long currentRow = 0;
                while ((line = reader.readLine()) != null) {
                    String[] row = line.split(",");
                    String key = row[0];
                    String[] rowCut = line.replace(key + ",", "").split(",");
                    rawMap.put(Long.parseLong(row[0]), rowCut);
                    currentRow += 1;
                    getProgressContainer().set((double) (currentRow / fileSize));
                }
            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }
            return this.dataHash = rawMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.progressActive = false;
        return new HashMap<>();
    }

    /**
     * Have this specify the default path when overridden like in the Stock class
     *
     * @return
     */
    public HashMap<Long, String[]> loadHistoricalData() {
        return loadHistoricalData(MAIN_PATH, getDefaultFileName());
    }


    /**
     * Saves all historical data to a specified path with the key as the datetimeID in the first column
     *
     * @param filePath
     * @param fileName
     * @return
     */
    public boolean saveHistoricalData(String filePath, String fileName) {
        return CSVManager.saveAsCSV(getHistoricalData(), filePath, fileName);
    }

    /**
     * Have this specify the default path when overridden like in the Stock class
     *
     * @return
     */
    public boolean saveHistoricalData() {
        return saveHistoricalData(MAIN_PATH, getDefaultFileName());
    }


    public abstract String getDefaultFileName();

    public float getLiveOpenValue() {
        return open;
    }

    public float getOpenValue(DateTime dateTime) {
        try {
            if (dateTime == null || dateTime.equals(getStock().getOpenTime())) {
                //return getLiveOpenValue();
                return calculateData(getStock().getOpenTime())[0];
            } else {
                return Float.parseFloat(getHistoricalData().get(dateTime.getDateTimeID())[0]);
            }
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public float getLiveCloseValue() {
        return close;
    }

    public float getCloseValue(DateTime dateTime) {
        try {
            if (dateTime == null || dateTime.equals(getStock().getOpenTime())) {
                //return getLiveCloseValue();
                return calculateData(getStock().getOpenTime())[1];
            } else {
                return Float.parseFloat(getHistoricalData().get(dateTime.getDateTimeID())[1]);
            }
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public Container<Double> getProgressContainer() {
        return progressContainer;
    }

    public boolean isProgressActive() {
        return progressActive;
    }


    public HashMap<Long, String[]> getHistoricalData() {
        return dataHash;
    }

    public Stock getStock() {
        return stock;
    }


    private static double getDateTimePercent(DateTime start, DateTime current, DateTime end) {
        double year = current.getYear();
        double yearDiff = end.getYear() - start.getYear();
        double yearRange = 1 + yearDiff;

        double month = current.getMonth();
        double monthDiff = end.getMonth() - start.getMonth();
        double monthRange = 1 + (year == end.getYear() ? monthDiff : 12);

        double day = current.getDay();
        double dayDiff = end.getDay() - start.getDay();
        double dayRange = 1 + (month == end.getMonth() ? dayDiff : 31);

        double yearPercent = (year - start.getYear()) / yearRange;
        double monthPercent = (month - start.getMonth()) / monthRange / yearRange;
        double dayPercent = (day - start.getDay()) / dayRange / monthRange / yearRange;

        return yearPercent + monthPercent + dayPercent;
    }

    protected String getStockLabel() {
        return getStock().getTicker() + "_" + getStock().getTimeFrame().getTag();
    }

    //TODO: make data source options
    enum DataSource {
        SOURCE1
    }
}
