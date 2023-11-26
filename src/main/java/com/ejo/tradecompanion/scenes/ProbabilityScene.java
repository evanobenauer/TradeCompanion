package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.util.Key;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.render.CandleUI;
import com.ejo.stockdownloader.util.DrawUtil;
import com.ejo.stockdownloader.util.TimeFrame;

import java.util.ArrayList;
import java.util.Map;

public class ProbabilityScene extends Scene {

    Stock stock = new Stock("SPY", TimeFrame.ONE_MINUTE,true, Stock.PriceSource.MARKETWATCH);
    TextUI text = new TextUI("", Fonts.getDefaultFont(30),Vector.NULL,ColorE.WHITE);

    public ProbabilityScene() {
        super("Probability Scene");
        addElements(text);
    }

    ArrayList<Long> dateTimeIDList = new ArrayList<>();

    DateTime time = new DateTime(2023,9,21,11,04,00);
    double x = 3;

    @Override
    public void draw() {
        drawBackground(new ColorE(50, 50, 50, 255));
        super.draw();

        CandleUI candle = new CandleUI(stock,time,3,100,stock.getOpen(time),30,new Vector(1,200));
        candle.tick(this);
        candle.draw(this);
        if (candle.isMouseOver()) DrawUtil.drawCandleTooltip(candle,getWindow().getScaledMousePos());
        QuickDraw.drawText(candle.getOpenTime().toString(),Fonts.getDefaultFont(20),candle.getPos().getAdded(candle.getWidth(),0),ColorE.WHITE);

        double x = 3 + this.x;
        for (Long id : dateTimeIDList) {
            if (x > getSize().getX()) continue;
            DateTime time = new DateTime(id);
            CandleUI candleUI = new CandleUI(stock, time, x, 300, stock.getOpen(time), 30, new Vector(1, 200));
            if (x + 30 > 0) {
                candleUI.draw(this);
                candleUI.tick(this);
                if (candleUI.isMouseOver()) DrawUtil.drawCandleTooltip(candleUI, getWindow().getScaledMousePos());
            }
            x += candleUI.getWidth() + 3;
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (action != Key.ACTION_RELEASE) {
            if (key == Key.KEY_RIGHT.getId()) x -= 20;
            if (key == Key.KEY_LEFT.getId()) x += 20;
            if (key == Key.KEY_J.getId()) x = 0;
        }

        if (action != Key.ACTION_PRESS) return;
        if (key == Key.KEY_C.getId()) {
            dateTimeIDList.clear();
            //This is the example datapoint candle I am using

            // ----------------------------------------
            //EDITABLE DATA
            time = new DateTime(2014,7,14,14,13,0);
            double maxPercentDiff = (double) 20 /100;
            boolean doPriceScaling = true;
            // ----------------------------------------

            float open = stock.getOpen(time);
            float close = stock.getClose(time);
            float min = stock.getMin(time);
            float max = stock.getMax(time);
            float ocDiff = getOpenCloseDifference(open,close);
            float topWick = getTopWickSize(max,open,close);
            float bottomWick = getBottomWickSize(min,open,close);


            //TODO: Figure out why candles that exist around new years are RARELY not accurately similar... weird
            int similarCandleCount = 0;
            for (Map.Entry<Long, String[]> data : stock.getHistoricalData().entrySet()) { //Loops through all stock data
                float dataOpen = Float.parseFloat(data.getValue()[0]);
                float dataClose = Float.parseFloat(data.getValue()[1]);
                float dataMin = Float.parseFloat(data.getValue()[2]);
                float dataMax = Float.parseFloat(data.getValue()[3]);

                //This value scales based on the total cap of the stock over history so similarities have a chance
                float pricingScale = doPriceScaling ? dataOpen / open : 1;

                //TODO: Maybe replace this with "isWithinMargin" where you specify the price margin it has to be within and that can be scaled based off of price scaling
                boolean bodySize = isWithinDifference(getOpenCloseDifference(dataOpen,dataClose),ocDiff * pricingScale,maxPercentDiff);
                boolean topWickSize = isMagnitudeWithinDifference(getTopWickSize(dataMax,dataOpen,dataClose),topWick * pricingScale,maxPercentDiff);
                boolean bottomWickSize = isMagnitudeWithinDifference(getBottomWickSize(dataMin,dataOpen,dataClose),bottomWick * pricingScale,maxPercentDiff);

                if (bodySize && topWickSize && bottomWickSize) {
                    dateTimeIDList.add(data.getKey());
                    similarCandleCount ++;
                }
            }
            text.setText(String.valueOf(similarCandleCount));
        }
    }

    private float getOpenCloseDifference(float open, float close) {
        return close - open;
    }

    private float getTopWickSize(float max, float open, float close) {
        return getOpenCloseDifference(open,close) >= 0 ? max - close : max - open;
    }

    private float getBottomWickSize(float min, float open, float close) {
        return getOpenCloseDifference(open,close) <= 0 ? close - min : open - min;
    }

    private boolean isMagnitudeWithinDifference(float val1, float val2, double maxPercentDifference) {
        return isWithinDifference(Math.abs(val1),Math.abs(val2),maxPercentDifference);
    }

    private boolean isWithinDifference(float val1, float val2, double maxPercentDifference) {
        double avg = (val1 + val2) / 2; //AVG can be zero if BOTH val are 0 OR if val1 == -val2
        if (avg == 0) return val1 == 0; //This protects against infinite percent error for 0 values due to /0 error
        double percentDiff = Math.abs((val1 - val2) / avg);
        return percentDiff < maxPercentDifference;
    }

    private boolean isMagnitudeWithinError(float trueValue, float experimentalValue, double maxPercentError) {
        return isWithinError(Math.abs(trueValue),Math.abs(experimentalValue),maxPercentError);
    }

    private boolean isWithinError(float trueValue, float experimentalValue, double maxPercentError) {
        double percentDiff = Math.abs((experimentalValue - trueValue) / trueValue);
        return percentDiff < maxPercentError;
    }

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
    }
}
