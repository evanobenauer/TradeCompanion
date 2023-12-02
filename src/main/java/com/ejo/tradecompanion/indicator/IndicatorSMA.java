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

    public IndicatorSMA(Stock stock, int period) {
        super(stock);
        this.period = period;
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
        if (!StockUtil.isPriceActive(getStock().isExtendedHours(),dateTime)) return new float[]{-1,-1};
        //SMA CALCULATION
        ArrayList<Float> openAvgList = new ArrayList<>();
        ArrayList<Float> closeAvgList = new ArrayList<>();

        //Adds the period of candles behind our goal candle together to the avg list
        int candleCount = 0;
        int loopCount = 0;
        while (candleCount < getPeriod()) {
            DateTime nextDate = new DateTime(dateTime.getYear(),dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(),dateTime.getSecond() - loopCount * getStock().getTimeFrame().getSeconds());

            if (!StockUtil.isPriceActive(getStock().isExtendedHours(),nextDate)) {
                loopCount++;
                continue;
            }

            float[] data = getStock().getData(nextDate);
            float open = data[0];
            float close = data[1];
            if (open != -1) openAvgList.add(open);
            if (close != -1) closeAvgList.add(close);

            candleCount++;
            loopCount++;
        }

        //This code uses the previous SMA if there is a null price for the current dateTime. It is fairly useless
        //float open = getStock().getOpen(dateTime);
        //float close = getStock().getClose(dateTime);
        //float prevOpenSMA = Float.parseFloat(getHistoricalData().get(dateTime.getDateTimeID())[0]);
        //float prevCloseSMA = Float.parseFloat(getHistoricalData().get(dateTime.getDateTimeID())[1]);
        //float openAvg = (float) MathE.roundDouble(open == -1 ? prevOpenSMA : calculateAverage(openAvgList),4);
        //float closeAvg = (float) MathE.roundDouble(close == -1 ? prevCloseSMA : calculateAverage(closeAvgList),4);

        float openAvg = (float) MathE.roundDouble(calculateAverage(openAvgList),4);
        float closeAvg = (float) MathE.roundDouble(calculateAverage(closeAvgList),4);
        getHistoricalData().put(dateTime.getDateTimeID(), new float[]{openAvg ,closeAvg});
        this.currentCalculationDate = dateTime;

        return new float[]{openAvg,closeAvg};
    }

    @Override
    public String getDefaultFileName() {
        return "SMA" + getPeriod() + "-" + getStockLabel() + (getStock().isExtendedHours() ? "-EH" : "");
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
