package com.ejo.tradecompanion.data;

import com.ejo.glowlib.misc.DoOnce;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowlib.util.TimeUtil;
import com.ejo.tradecompanion.util.StockUtil;
import com.ejo.tradecompanion.util.TimeFrame;
import com.ejo.tradecompanion.web.StockScraper;

import java.io.*;

/**
 * The stock class is a multi use class. It encompasses both loading historical data and adding new data to said history. The live data is updated
 * by a method whenever it is called
 * Historical data is saved in the folder: stock_data/ticker_timeframe.csv. Place data into this location for it to be
 * saved and loaded with the stock
 */
public class Stock extends HistoricalDataContainer {

    //Stock Information
    private final String ticker;
    private final TimeFrame timeFrame;
    private final boolean extendedHours;
    private StockScraper.PriceSource livePriceSource;

    //Open Time
    private DateTime openTime;

    //Open-Close Percentage
    private final Container<Double> closePercent;

    //Live Price Variables
    private float price;
    private float open;
    private float min;
    private float max;

    //Live Price Update Variables
    private boolean shouldStartUpdates;
    private final StopWatch updateTimer = new StopWatch();

    //Do Once Definitions
    private final DoOnce doLivePriceUpdate = new DoOnce();
    private final DoOnce doOpen = new DoOnce();
    private final DoOnce doClose = new DoOnce();

    //Default Constructor
    public Stock(String ticker, TimeFrame timeFrame, boolean extendedHours, StockScraper.PriceSource livePriceSource, boolean loadOnInstantiation) {
        this.ticker = ticker;
        this.timeFrame = timeFrame;
        this.extendedHours = extendedHours;
        this.livePriceSource = livePriceSource;

        if (loadOnInstantiation) this.dataHash = loadHistoricalData();

        this.setAllData(-1);
        this.closePercent = new Container<>(0d);
        this.shouldStartUpdates = false;

        this.doLivePriceUpdate.reset();
        this.doOpen.reset();
        this.doClose.reset();
    }

    public Stock(String ticker, TimeFrame timeFrame, boolean extendedHours, StockScraper.PriceSource livePriceSource) {
        this(ticker, timeFrame, extendedHours, livePriceSource, true);
    }


    /**
     * This method updates the live price of the stock as well as the min and max. Depending on the timeframe, the stock will save data to the dataList periodically with this method
     * **METHOD PROCESS: Waits... [Time to close: Updates the close, updates the price, updates the open], Waits...**
     */
    public void updateLiveData(double liveDelayS, boolean includePriceUpdate) {
        //Updates the progress bar of each segmentation
        if (StockUtil.isPriceActive(isExtendedHours(), TimeUtil.getAdjustedCurrentTime())) updateClosePercent();

        //Check if the stock should update. If not, don't run the method
        if (!shouldUpdate()) return;

        //Close the previous segment
        updateClose();

        //Update live price every provided delay second or update the live price on the start of every open
        if (includePriceUpdate) updateLivePrice(liveDelayS);

        //Open the next segment
        updateOpen();

        //Updates the minimum/maximum values of the stock price over the time frame
        updateMinMax();
    }

    public void updateLiveData() {
        updateLiveData(0, false);
    }


    /**
     * Retrieves and sets the live price data gathered for the stock from web scraping.
     */
    public void updateLivePrice() {
        try {
            StockScraper scraper = new StockScraper(this);
            float livePrice = scraper.scrapeLivePrice(getLivePriceSource());
            if (livePrice != -1) this.price = livePrice;
        } catch (IOException e) {
            System.out.println("Live Data: Timed Out");
        }
    }


    /**
     * Updates the live price data every timeframe specified in the liveDelay in seconds. The method will also force an update at the beginning of every open to make sure the stock
     * is up-to-date.
     * It is best to include this update in a parallel thread as the price scraping from the internet may cause lag
     *
     * @param liveDelayS
     */
    public void updateLivePrice(double liveDelayS) {
        updateTimer.start();
        if (updateTimer.hasTimePassedS(liveDelayS) || shouldClose()) {
            doLivePriceUpdate.run(() -> {
                updateTimer.restart();
                updateLivePrice();
            });
        }

        //Have live price updates reset if the stock should not close to continue with the liveDelay. This is so the stock will FORCE an update each open. Shown above
        if (!shouldClose()) doLivePriceUpdate.reset();
    }


    /**
     * Sets the stock's open, min, and max to the current price value only when doOpen is set to reset
     */
    private void updateOpen() {
        this.doOpen.run(() -> {
            this.openTime = TimeUtil.getAdjustedCurrentTime();
            setAllData(getPrice());
        });
    }


    /**
     * Updates the splitting of the stock into candles based on the TimeFrame of the stock selected. This method adds an entry to the historical data HashMap and then resets the livedata to the current price
     */
    private void updateClose() {
        if (!shouldClose()) {
            doClose.reset();
            return;
        }
        this.doClose.run(() -> {
            DateTime ct = TimeUtil.getAdjustedCurrentTime();
            //Save Live Data as Historical [Data is stored as (DATETIME,OPEN,CLOSE,MIN,MAX)]
            float[] timeFrameData = {getOpen(), getPrice(), getMin(), getMax()};
            DateTime openTime = new DateTime(ct.getYear(), ct.getMonth(), ct.getDay(), ct.getHour(), ct.getMinute(), ct.getSecond() - getTimeFrame().getSeconds());
            if (getOpenTime() != null) dataHash.put(openTime.getDateTimeID(), timeFrameData);

            //Set stock ready for open
            doOpen.reset();
        });
    }


    /**
     * Updates the minimum/maximum values of the stock over the time frame period. This is reset upon open
     */
    private void updateMinMax() {
        if (getOpenTime() == null) return;
        if (getPrice() < getMin()) this.min = getPrice();
        if (getPrice() > getMax()) this.max = getPrice();
    }


    /**
     * Updates the percentage complete for the current stock candle
     */
    private void updateClosePercent() {
        DateTime ct = TimeUtil.getAdjustedCurrentTime();
        double totalPercent = 0;

        //Second Percent
        double secPercent = (double) ct.getSecond() / getTimeFrame().getSeconds();
        totalPercent += secPercent;

        //Minute Percent
        double minPercent = ct.getMinute() / ((double) getTimeFrame().getSeconds() / 60);
        totalPercent += minPercent;

        //Hour Percent
        double hrPercent = ct.getHour() / ((double) getTimeFrame().getSeconds() / 60 / 60);
        totalPercent += hrPercent;

        totalPercent -= Math.floor(totalPercent);
        getClosePercent().set(totalPercent);
    }


    /**
     * Checks if the stock should update live data. This method has the main purpose of stopping the update method if returned false
     *
     * @return
     */
    public boolean shouldUpdate() {
        //Wait until the start of the candle timeframe to allow updates
        if (shouldClose()) this.shouldStartUpdates = true;
        if (!this.shouldStartUpdates) return false;

        //Only allows for data collection during trading hours
        return StockUtil.isPriceActive(isExtendedHours(), TimeUtil.getAdjustedCurrentTime());

        //Finally, if all checks pass,
        //return true;
    }

    /**
     * This method will return true if the stock is at a place to go through with a split depending on the current TimeFrame
     *
     * @return
     */
    public boolean shouldClose() {
        DateTime ct = TimeUtil.getAdjustedCurrentTime();
        return StockUtil.shouldClose(ct,getTimeFrame());
    }

    /**
     * Sets all the data pertaining to the stock to a single value. This includes the price, open, min, and max
     *
     * @param value
     */
    private void setAllData(float value) {
        this.price = value;
        this.open = value;
        this.min = value;
        this.max = value;
    }

    public void setLivePriceSource(StockScraper.PriceSource livePriceSource) {
        this.livePriceSource = livePriceSource;
    }


    @Override
    public String getDefaultFileName() {
        return getTicker() + "_" + getTimeFrame().getTag();
    }

    @Override
    public String getDefaultFilePath() {
        return "stock_data";
    }

    /**
     * Returns the raw data from the historical hashmap.
     * This is in the format of: Open, Close, Min, Max, Volume
     * @param dateTime
     * @return
     */
    public float[] getData(DateTime dateTime) {
        float[] rawData = getHistoricalData().get(dateTime.getDateTimeID());
        if (rawData == null)
            return dateTime.equals(getOpenTime()) ? new float[]{getOpen(),getPrice(),getMin(),getMax()} : new float[]{-1,-1,-1,-1,-1};
        return rawData;
    }

    public float getOpen() {
        return open;
    }

    public float getOpen(DateTime dateTime) {
        return getData(dateTime)[0];
    }

    public float getPrice() {
        return price;
    }

    public float getClose(DateTime dateTime) {
        return getData(dateTime)[1];
    }

    public float getMin() {
        return min;
    }

    public float getMin(DateTime dateTime) {
        return getData(dateTime)[2];
    }

    public float getMax() {
        return max;
    }

    public float getMax(DateTime dateTime) {
        return getData(dateTime)[3];
    }


    public Container<Double> getClosePercent() {
        return closePercent;
    }

    public DateTime getOpenTime() {
        return openTime;
    }

    public StockScraper.PriceSource getLivePriceSource() {
        return livePriceSource;
    }

    public boolean isExtendedHours() {
        return extendedHours;
    }

    public TimeFrame getTimeFrame() {
        return timeFrame;
    }

    public String getTicker() {
        return ticker;
    }

}
