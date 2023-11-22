package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.util.Key;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.render.CandleUI;
import com.ejo.stockdownloader.util.StockDrawUtil;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.indicator.IndicatorSMA;

import java.util.ArrayList;

public class UpDownScene extends Scene {

    Stock stock = new Stock("SPY", TimeFrame.ONE_MINUTE,true, Stock.PriceSource.MARKETWATCH,false);

    IndicatorSMA SMA = new IndicatorSMA(stock,false,50);

    ProgressBarUI<Double> progressBar = new ProgressBarUI<>(new Vector(200,200),new Vector(300,50),ColorE.BLUE,stock.getClosePercent(),0,1);
    ProgressBarUI<Double> progressBar2 = new ProgressBarUI<>(new Vector(0,0),new Vector(300,25),ColorE.BLUE,SMA.getCalculationPercent(),0,1);

    public UpDownScene() {
        super("UpDown Scene");
        stock.loadHistoricalData("stock_data","SPY_1min-01-2000-02-2017");
        SMA.loadHistoricalData();
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

        addElements(progressBar,progressBar2);
    }


    @Override
    public void draw() {
        drawBackground(new ColorE(50, 50, 50, 255));
        QuickDraw.drawRect(progressBar.getPos(),progressBar.getSize(),ColorE.BLACK);
        QuickDraw.drawRect(progressBar2.getPos(),progressBar2.getSize(),ColorE.BLACK);
        super.draw();
        QuickDraw.drawTextCentered(String.valueOf(stock.getPrice()), Fonts.getDefaultFont(40), Vector.NULL,getSize(), ColorE.WHITE);

        drawCandles(this,stock, stock.getPrice(),300,20,50,new Vector(1,1000));
    }

    @Override
    public void tick() {
        super.tick();
        stock.updateLiveData();
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (action != Key.ACTION_PRESS) return;
        if (key == Key.KEY_S.getId()) {
            //stock.saveHistoricalData();
            SMA.saveHistoricalData();
        }
        if (key == Key.KEY_C.getId()) {
            Thread thread = new Thread(() -> SMA.calculateData(new DateTime(2016, 1, 3, 4, 0), new DateTime(2016, 10, 7, 19, 59)));
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void drawCandles(Scene scene, Stock stock, double focusPrice, double focusY, double separation, double candleWidth, Vector candleScale) {
        //Define Candle List
        ArrayList<CandleUI> candleList = new ArrayList<>();

        //Create Live Candle
        CandleUI liveCandle = new CandleUI(stock, getSize().getX() - candleWidth - separation, focusY, focusPrice, candleWidth, candleScale);
        candleList.add(liveCandle);

        //Create Historical Candles
        DateTime ot = stock.getOpenTime();
        int candleAmount = (int) (getSize().getX() / 18) + 1;
        for (int i = 1; i < candleAmount; i++) {
            DateTime candleTime = new DateTime(ot.getYearInt(), ot.getMonthInt(), ot.getDayInt(), ot.getHourInt(), ot.getMinuteInt(), ot.getSecondInt() - stock.getTimeFrame().getSeconds() * i);
            CandleUI historicalCandle = new CandleUI(stock, candleTime, liveCandle.getPos().getX() - (separation + candleWidth) * i, focusY, focusPrice, candleWidth, candleScale);
            candleList.add(historicalCandle);
        }

        //Draw Candles
        for (CandleUI candle : candleList) {
            if (candle.getStock().getOpen(candle.getOpenTime()) != -1) {
                candle.draw();
                candle.tick(scene); //Update Mouse Over
                if (candle.isMouseOver()) StockDrawUtil.drawCandleTooltip(candle, getWindow().getScaledMousePos());
            }
        }

        //Draw Tooltips
        for (CandleUI candle : candleList) {
            if (candle.getStock().getOpen(candle.getOpenTime()) != -1) {
                candle.tick(scene); //Update Mouse Over
                if (candle.isMouseOver()) StockDrawUtil.drawCandleTooltip(candle, getWindow().getScaledMousePos());
            }
        }

    }
}

