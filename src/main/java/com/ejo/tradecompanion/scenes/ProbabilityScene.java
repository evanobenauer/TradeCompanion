package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.Vector;
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
            float open;
            float close;
            float min;
            float max;
            for (Map.Entry<Long, String[]> data : stock.getHistoricalData().entrySet()) { //Loops through all stock data
                //Use this loop to find all similar candles and their datetimes
            }
        }
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
