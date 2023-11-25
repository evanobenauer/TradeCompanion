package com.ejo.tradecompanion;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.Window;
import com.ejo.tradecompanion.scenes.ProbabilityScene;
import com.ejo.tradecompanion.scenes.TestScene;
import com.ejo.tradecompanion.scenes.UpDownScene;

public class App {

    //TODO: Have Probability Trading
    // Have Chart View Menu
    // Import StockDownloader as a dependency
    // Have practice mode where you can mark the start of the trade and watch if it goes up or down

    private static final Window window = new Window(
            "Trade Companion",
            new Vector(100,100),
            new Vector(1200,800),
            new ProbabilityScene(),
            true, 4, 60, 60
    );

    public static void main(String[] args) {
        window.run();
        window.close();
    }
}