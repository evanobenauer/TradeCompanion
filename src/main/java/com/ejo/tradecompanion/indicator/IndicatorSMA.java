package com.ejo.tradecompanion.indicator;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class IndicatorSMA extends Indicator {

    private final int period;

    private DateTime currentCalculationDate;

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
    public float[] calculateData(DateTime dateTime) {
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

            float open = getStock().getOpen(nextDate);
            float close = getStock().getClose(nextDate);
            if (open != -1) openAvgList.add(open);
            if (close != -1) closeAvgList.add(close);

            candleCount++;
            loopCount++;
        }

        float openAvg = (float) MathE.roundDouble(calculateAverage(openAvgList),4);
        float closeAvg = (float) MathE.roundDouble(calculateAverage(closeAvgList),4);
        getHistoricalData().put(dateTime.getDateTimeID(), new String[]{String.valueOf(openAvg) ,String.valueOf(closeAvg)});
        this.currentCalculationDate = dateTime;

        return new float[]{openAvg,closeAvg};
    }

    @Override
    public String getDefaultFileName() {
        return "SMA" + getPeriod() + "-" + STOCK_LABEL + (getStock().isExtendedHours() ? "-EH" : "");
    }


    public DateTime getCurrentCalculationDate() {
        return currentCalculationDate;
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
