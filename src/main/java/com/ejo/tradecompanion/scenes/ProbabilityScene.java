package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.util.Key;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.TimeFrame;

import java.util.Map;

public class ProbabilityScene extends Scene {

    Stock stock = new Stock("SPY", TimeFrame.ONE_MINUTE,true, Stock.PriceSource.MARKETWATCH);

    public ProbabilityScene() {
        super("Probability Scene");
    }

    @Override
    public void draw() {
        super.draw();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (action != Key.ACTION_PRESS) return;
        if (key == Key.KEY_C.getId()) {
            //This is the example datapoint candle I am using
            //20230921104000,435.3180,435.2000,435.0650,435.3250,282028
            float open = 435.3180f;
            float close = 435.2f;
            float ocDiff = getOpenCloseDifference(open,close);
            float min = 435.0650f;
            float max = 435.3250f;
            float topWick = getTopWickSize(max,open,close);
            float bottomWick = getBottomWickSize(min,open,close);

            double maxPercentDiff = .01;

            int similarCandleCount = 0;

            System.out.println("Started");
            for (Map.Entry<Long, String[]> data : stock.getHistoricalData().entrySet()) { //Loops through all stock data
                float dataOpen = Float.parseFloat(data.getValue()[0]);
                float dataClose = Float.parseFloat(data.getValue()[1]);
                float dataMin = Float.parseFloat(data.getValue()[2]);
                float dataMax = Float.parseFloat(data.getValue()[3]);

                if (isWithinError(getOpenCloseDifference(dataOpen,dataClose),ocDiff,maxPercentDiff)
                        && isWithinError(getTopWickSize(dataMax,dataOpen,dataClose),topWick,maxPercentDiff)
                        && isWithinError(getBottomWickSize(dataMin,dataOpen,dataClose),bottomWick,maxPercentDiff)
                ) {
                    System.out.println(new DateTime(data.getKey()));
                    similarCandleCount ++;
                }
            }
            System.out.println("done: " + similarCandleCount);

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

    private boolean isWithinError(float val1, float val2, double maxPercentDifference) {
        double avg = (val1 + val2) / 2;
        double percentDiff = Math.abs((val1 - val2) / avg);
        return percentDiff < maxPercentDifference;
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
