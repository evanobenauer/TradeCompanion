package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.util.StockUtil;

import java.util.ArrayList;

public class IndicatorSMA extends IndicatorMA {

    private DateTime currentCalculationDate;

    public IndicatorSMA(Stock stock, int period, Type type, ColorE color, int lineWidth) {
        super(stock,false,period, type, color, lineWidth);
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
            DateTime nextDate = dateTime.getAdded(-loopCount * getStock().getTimeFrame().getSeconds());
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

        float openAvg = (float) MathE.roundDouble(calculateAverage(openAvgList),4);
        float closeAvg = (float) MathE.roundDouble(calculateAverage(closeAvgList),4);
        float[] result = new float[]{openAvg ,closeAvg};
        getHistoricalData().put(dateTime.getDateTimeID(), result);
        this.currentCalculationDate = dateTime;

        return result;
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

    @Override
    public String toString() {
        return "SMA" + getPeriod();
    }
}
