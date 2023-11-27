package com.ejo.tradecompanion.scenes.experimental;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.util.Key;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.indicator.IndicatorEMA;
import com.ejo.tradecompanion.indicator.IndicatorSMA;
import com.ejo.tradecompanion.util.RenderUtil;

public class TestScene extends Scene {

    Stock stock = new Stock("SPY", TimeFrame.ONE_MINUTE,true, Stock.PriceSource.MARKETWATCH,false);

    IndicatorSMA SMA = new IndicatorSMA(stock,false,50);
    IndicatorEMA EMA = new IndicatorEMA(stock,false,50);

    ProgressBarUI<Double> progressBar = new ProgressBarUI<>(new Vector(200,200),new Vector(300,50),ColorE.BLUE,stock.getClosePercent(),0,1);
    ProgressBarUI<Double> progressBar2 = new ProgressBarUI<>(new Vector(0,0),new Vector(300,25),ColorE.BLUE,SMA.getProgressContainer(),0,1);
    ProgressBarUI<Double> progressBar3 = new ProgressBarUI<>(new Vector(0,30),new Vector(300,25),ColorE.BLUE,EMA.getProgressContainer(),0,1);

    public TestScene() {
        super("Test Scene");
        stock.loadHistoricalData("stock_data","SPY_1min.csv");
        SMA.loadHistoricalData();
        EMA.loadHistoricalData();
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

        addElements(progressBar,progressBar2,progressBar3);
    }

    int candleOffset = 0;
    int candleYOffset = 0;
    double candleScaleX = 1;
    double candleScaleY = 500;

    @Override
    public void draw() {
        drawBackground(new ColorE(50, 50, 50, 255));
        progressBar.setPos(new Vector(0,getSize().getY() - progressBar.getSize().getY()));
        QuickDraw.drawRect(progressBar.getPos(),progressBar.getSize(),ColorE.BLACK);
        QuickDraw.drawRect(progressBar2.getPos(),progressBar2.getSize(),ColorE.BLACK);
        QuickDraw.drawRect(progressBar3.getPos(),progressBar3.getSize(),ColorE.BLACK);
        super.draw();
        QuickDraw.drawText(String.valueOf(stock.getPrice()), Fonts.getDefaultFont(40), new Vector(2, progressBar.getPos().getY() - 42), ColorE.WHITE);

        if (SMA.isProgressActive() && SMA.getCurrentCalculationDate() != null) QuickDraw.drawText(SMA.getCurrentCalculationDate().toString(),Fonts.getDefaultFont(20),new Vector(progressBar2.getSize().getX() + 4,progressBar2.getPos().getY()),ColorE.WHITE);
        if (EMA.isProgressActive() && EMA.getCurrentCalculationDate() != null) QuickDraw.drawText(EMA.getCurrentCalculationDate().toString(),Fonts.getDefaultFont(20),new Vector(progressBar3.getSize().getX() + 4,progressBar3.getPos().getY()),ColorE.WHITE);

        DateTime time = new DateTime(20231113113000L).getAdded(candleOffset * 60); //new DateTime(2023,11,22,15,59 + candleOffset)
        double focusPrice = EMA.getCloseValue(time);
        RenderUtil.drawStockData(this,stock,time,600,focusPrice,getSize().getY() / 2 + candleYOffset,6 * candleScaleX,20,new Vector(candleScaleX,candleScaleY),EMA,SMA);
    }


    @Override
    public void tick() {
        super.tick();
        stock.updateLiveData();
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (action == Key.ACTION_RELEASE) return;
        int speed = Key.isShiftDown() ? 10 : 1;
        if (key == Key.KEY_J.getId()) candleOffset = 0;
        if (key == Key.KEY_LEFT.getId()) candleOffset -= speed;
        if (key == Key.KEY_RIGHT.getId()) candleOffset += speed;

        if (key == Key.KEY_DOWN.getId()) candleYOffset -= speed;
        if (key == Key.KEY_UP.getId()) candleYOffset += speed;

        if (key == Key.KEY_EQUALS.getId()) candleScaleY += speed * 30;
        if (key == Key.KEY_MINUS.getId()) candleScaleY -= speed * 30;

        if (action != Key.ACTION_PRESS) return;
        if (key == Key.KEY_S.getId()) {
            //stock.saveHistoricalData();
            SMA.saveHistoricalData();
            EMA.saveHistoricalData();
        }
        if (key == Key.KEY_C.getId()) {
            Thread thread = new Thread(() -> {
                //SMA.calculateData(new DateTime(2023, 11, 1, 4, 0), new DateTime(2023, 11, 24, 19, 59));
                //EMA.calculateData(new DateTime(2004, 11, 1, 4, 0), new DateTime(2023, 11, 24, 19, 59));
                EMA.calculateData();
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
        candleScaleX += (double) scroll / 100;
        if (candleScaleX < 0.1) candleScaleX = .1;
    }

}

