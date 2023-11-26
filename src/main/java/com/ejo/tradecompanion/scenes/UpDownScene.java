package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.indicator.IndicatorSMA;
import com.ejo.tradecompanion.util.RenderUtil;

public class UpDownScene extends Scene {


    Stock stock = new Stock("SPY", TimeFrame.ONE_MINUTE,true, Stock.PriceSource.MARKETWATCH,false);

    IndicatorSMA SMA = new IndicatorSMA(stock,false,50);

    ProgressBarUI<Double> progressBar = new ProgressBarUI<>(new Vector(200,200),new Vector(300,50),ColorE.BLUE,stock.getClosePercent(),0,1);

    public UpDownScene() {
        super("UpDown Scene");
        addElements(progressBar);
        Thread stockUpdateThread = new Thread(() -> {
            while (true) {
                stock.updateLivePrice(.5);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        stockUpdateThread.setDaemon(true);
        stockUpdateThread.setName("Stock Update Thread");
        stockUpdateThread.start();
    }

    @Override
    public void draw() {
        super.draw();
        double candleWidth = 14;
        double candleSpace = 4;
        double focusY = getSize().getY() / 2;
        double focusPrice = stock.getPrice();
        Vector candleScale = new Vector(1, 200);
        RenderUtil.drawStockData(this,stock, StockUtil.getAdjustedCurrentTime(),100,focusPrice,focusY,candleSpace,candleWidth,candleScale);
    }

    @Override
    public void tick() {
        super.tick();
        stock.updateLiveData();
    }

}
