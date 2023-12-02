package com.ejo.tradecompanion.indicator;

import com.ejo.glowlib.file.CSVManager;
import com.ejo.glowlib.file.FileManager;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

public abstract class Indicator {

    //File Path
    protected static final String MAIN_PATH = "stock_data/indicator_data";

    //Stock
    private final Stock stock;

    //Historical Data HashMap
    private HashMap<Long, float[]> dataHash = new HashMap<>();

    //Live Scrape Data Variables
    private final StopWatch updateTimer = new StopWatch();
    private float open;
    private float close;

    //Calculation Variables
    private final Container<Double> progressContainer = new Container<>(0d);
    protected boolean progressActive = false;


    public Indicator(Stock stock) {
        this.stock = stock;
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
    public HashMap<Long, float[]> loadHistoricalData(String filePath, String fileName) {
        this.progressActive = true;
        try {
            File file = new File(filePath + (fileName.equals("") ? "" : "/") + fileName.replace(".csv", "") + ".csv");
            HashMap<Long, float[]> rawMap = new HashMap<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                long fileSize = Files.lines(file.toPath()).count();
                long currentRow = 0;
                while ((line = reader.readLine()) != null) {
                    String[] row = line.split(",");
                    String key = row[0];
                    String[] rowCut = line.replace(key + ",", "").split(",");

                    float[] floatRowCut = new float[rowCut.length];
                    for (int i = 0; i < rowCut.length; i++) floatRowCut[i] = Float.parseFloat(rowCut[i]);

                    rawMap.put(Long.parseLong(row[0]), floatRowCut);
                    currentRow += 1;
                    getProgressContainer().set((double) (currentRow / fileSize));
                }
            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }
            this.progressActive = false;
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
    public HashMap<Long, float[]> loadHistoricalData() {
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
        this.progressActive = true;
        FileManager.createFolderPath(filePath); //Creates the folder path if it does not exist
        HashMap<Long, float[]> hashMap = getHistoricalData();
        String outputFile = filePath + (filePath.equals("") ? "" : "/") + fileName.replace(".csv","") + ".csv";
        long fileSize = hashMap.size();
        long currentRow = 0;
        try(FileWriter writer = new FileWriter(outputFile)) {
            for (Long key : hashMap.keySet()) {
                writer.write(key + "," + Arrays.toString(hashMap.get(key)).replace("[","").replace("]","").replace(" ","") + "\n");
                currentRow += 1;
                getProgressContainer().set((double) (currentRow / fileSize));
            }
            return true;
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
        this.progressActive = false;
        return false;
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

    public Container<Double> getProgressContainer() {
        return progressContainer;
    }

    public boolean isProgressActive() {
        return progressActive;
    }


    public HashMap<Long, float[]> getHistoricalData() {
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

}
