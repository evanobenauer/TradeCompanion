package com.ejo.tradecompanion;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.Window;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.scenes.ChartViewScene;
import com.ejo.tradecompanion.scenes.experimental.ProbabilityScene;

public class App {

    //New Todo list:
    // Create back testing mode
    // Create Up/Down mode

    private static final Window window = new Window(
            "Trade Companion",
            new Vector(100,100),
            new Vector(1200,800),
            new ChartViewScene(new Stock("SPY", TimeFrame.ONE_MINUTE,true, Stock.PriceSource.MARKETWATCH)),
            //new TestScene(),
            //new ProbabilityScene(),
            true, 4, 60, 60
    );

    public static void main(String[] args) {
        window.run();
        window.close();
    }
}