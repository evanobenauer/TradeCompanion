package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.Stock;

public abstract class IndicatorMA extends Indicator {

    private final int period;

    private Type type;
    private ColorE color;
    private int lineWidth;

    public IndicatorMA(Stock stock, boolean loadOnInstantiation, int period, Type type, ColorE color, int lineWidth) {
        super(stock, loadOnInstantiation);
        this.period = period;
        this.type = type;
        this.color = color;
        this.lineWidth = lineWidth;
    }

    @Override
    public float[] calculateData(DateTime dateTime) {
        return new float[0];
    }

    public IndicatorMA setColor(ColorE color) {
        this.color = color;
        return this;
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

    public float getOpenValue(DateTime dateTime) {
        if (dateTime.equals(getStock().getOpenTime())) return calculateData(getStock().getOpenTime())[0];
        return getData(dateTime)[0];
    }

    public float getCloseValue(DateTime dateTime) {
        if (dateTime.equals(getStock().getOpenTime())) return calculateData(getStock().getOpenTime())[1];
        return getData(dateTime)[1];
    }


    public int getPeriod() {
        return period;
    }


    public Type getType() {
        return type;
    }

    public ColorE getColor() {
        return color;
    }

    public int getLineWidth() {
        return lineWidth;
    }


    public enum Type {
        OPEN(0),
        CLOSE(1);

        final int index;
        Type(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

}
