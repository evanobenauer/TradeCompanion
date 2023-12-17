package com.ejo.tradecompanion.data.indicator;

import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.Stock;

public abstract class IndicatorMA extends Indicator {

    private final int period;

    private ColorE color;

    public IndicatorMA(Stock stock, boolean loadOnInstantiation, int period, ColorE color) {
        super(stock, loadOnInstantiation);
        this.period = period;
        this.color = color;
    }

    @Override
    public float[] calculateData(DateTime candleTime) {
        return new float[0];
    }

    public IndicatorMA setColor(ColorE color) {
        this.color = color;
        return this;
    }


    public int getPeriod() {
        return period;
    }

    public ColorE getColor() {
        return color;
    }
}
