package com.ejo.tradecompanion;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.Window;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.scenes.TitleScene;

import java.io.IOException;

public class App {

    //New Todo list:
    // Create back testing mode
    // Create Up/Down mode

    private static final Window window = new Window(
            "Trade Companion",
            new Vector(100,100),
            new Vector(1200,800),
            new TitleScene(),
            true, 4, 60, 60
    );

    public static void main(String[] args) throws IOException {
        window.run();
        window.close();
    }
}