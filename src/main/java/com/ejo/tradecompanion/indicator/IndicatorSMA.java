package com.ejo.tradecompanion.indicator;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class IndicatorSMA extends Indicator {

    private final int period;

    public IndicatorSMA(Stock stock, boolean loadOnInstantiation, int period) {
        super(stock, loadOnInstantiation);
        this.period = period;
    }

    public IndicatorSMA(Stock stock, int period) {
        this(stock,true,period);
    }

    @Override
    public void updateScrapeData() {
        //TODO: Figure this out lol
    }

    /**
     * //TODO: when there is a day off that's not a weekend, like a monday, the stock still adds a candleCount for each day. Don't let it do this
     * @param dateTime
     */
    @Override
    public void calculateData(DateTime dateTime) {
        //SMA CALCULATION
        ArrayList<Float> openAvgList = new ArrayList<>();
        ArrayList<Float> closeAvgList = new ArrayList<>();

        //Adds the period of candles behind our goal candle together to the avg list
        int candleCount = 0;
        int loopCount = 0;
        while (candleCount < getPeriod()) {
            DateTime nextDate = new DateTime(dateTime.getYearInt(),dateTime.getMonthInt(), dateTime.getDayInt(), dateTime.getHourInt(), dateTime.getMinuteInt(),dateTime.getSecondInt() - loopCount * getStock().getTimeFrame().getSeconds());

            if (!StockUtil.isPriceActive(getStock().isExtendedHours(),nextDate)) {
                loopCount++;
                continue;
            }

            try {
                openAvgList.add(getStock().getOpen(nextDate));
                closeAvgList.add(getStock().getClose(nextDate));
            } catch (NullPointerException e) { //Value is null
            }

            candleCount++;
            loopCount++;
        }

        double openAvg = MathE.roundDouble(calculateAverage(openAvgList),4);
        double closeAvg = MathE.roundDouble(calculateAverage(closeAvgList),4);
        long key = dateTime.getDateTimeID();
        //System.out.println(new DateTime(key).getFormattedDateTime()); //TODO: Replace this with a progress bar container
        getHistoricalData().put(key, new String[]{String.valueOf(openAvg), String.valueOf(closeAvg)});
    }

    @Override
    public HashMap<Long, String[]> loadHistoricalData() {
        return loadHistoricalData("stock_data/indicator_data",  "SMA" + getPeriod() + "-" + getStock().getTicker() + "_" + getStock().getTimeFrame() + (getStock().isExtendedHours() ? "-EH" : ""));
    }

    @Override
    public boolean saveHistoricalData() {
        return this.saveHistoricalData("stock_data/indicator_data","SMA" + getPeriod() + "-" + getStock().getTicker() + "_" + getStock().getTimeFrame() + (getStock().isExtendedHours() ? "-EH" : ""));
    }

    public static <T extends Number> double calculateAverage(ArrayList<T> values) {
        double avg = 0;
        for (T val : values) {
            avg += val.doubleValue();
        }
        avg /= values.size();
        return avg;
    }

    public int getPeriod() {
        return period;
    }
}
