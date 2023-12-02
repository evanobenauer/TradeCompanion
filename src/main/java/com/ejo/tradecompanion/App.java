package com.ejo.tradecompanion;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.Window;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.scenes.ChartViewScene;

public class App {

    //TODO: Have Probability Trading
    // Have Chart View Menu
    // Import StockDownloader as a dependency
    // Have practice mode where you can mark the start of the trade and watch if it goes up or down

    //New Todo list:
    // Create chart viewing scene
    // Create back testing mode
    // Finish Probability mode
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