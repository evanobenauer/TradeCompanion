package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.glowui.util.Key;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.render.CandleUI;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.indicator.IndicatorSMA;

import java.awt.*;
import java.util.ArrayList;

public class TestScene extends Scene {

    Stock stock = new Stock("SPY", TimeFrame.ONE_MINUTE,true, Stock.PriceSource.MARKETWATCH,false);

    IndicatorSMA SMA = new IndicatorSMA(stock,false,50);

    ProgressBarUI<Double> progressBar = new ProgressBarUI<>(new Vector(200,200),new Vector(300,50),ColorE.BLUE,stock.getClosePercent(),0,1);
    ProgressBarUI<Double> progressBar2 = new ProgressBarUI<>(new Vector(0,0),new Vector(300,25),ColorE.BLUE,SMA.getProgressContainer(),0,1);

    public TestScene() {
        super("Test Scene");
        stock.loadHistoricalData("stock_data","SPY_1min.csv");
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

    int candleOffset = 0;
    int candleYOffset = 0;

    @Override
    public void draw() {
        drawBackground(new ColorE(50, 50, 50, 255));
        QuickDraw.drawRect(progressBar.getPos(),progressBar.getSize(),ColorE.BLACK);
        QuickDraw.drawRect(progressBar2.getPos(),progressBar2.getSize(),ColorE.BLACK);
        super.draw();
        QuickDraw.drawTextCentered(String.valueOf(stock.getPrice()), Fonts.getDefaultFont(40), Vector.NULL,getSize(), ColorE.WHITE);

        if (SMA.isProgressActive() && SMA.getCurrentCalculationDate() != null) QuickDraw.drawText(SMA.getCurrentCalculationDate().getFormattedDateTime(),Fonts.getDefaultFont(20),new Vector(2,50),ColorE.WHITE);

        //drawCandles(this,stock, stock.getPrice(),300,20,50,new Vector(1,1000));
        drawCandles(stock,new DateTime(2023,11,22,15,59 + candleOffset),455,500 + candleYOffset,6,20,new Vector(1,500));
    }


    @Override
    public void tick() {
        super.tick();
        stock.updateLiveData();
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (key == Key.KEY_LEFT.getId()) candleOffset --;
        if (key == Key.KEY_RIGHT.getId()) candleOffset ++;

        if (action != Key.ACTION_PRESS) return;
        if (key == Key.KEY_S.getId()) {
            //stock.saveHistoricalData();
            SMA.saveHistoricalData();
        }
        if (key == Key.KEY_C.getId()) {
            Thread thread = new Thread(() -> SMA.calculateData(new DateTime(2023, 10, 3, 4, 0), new DateTime(2023, 11, 24, 19, 59)));
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
        candleYOffset += scroll * 10;
    }

    private void drawCandles(Stock stock, DateTime endTime, double focusPrice, double focusY, double separation, double candleWidth, Vector candleScale) {
        //Define Candle List
        ArrayList<CandleUI> listCandle = new ArrayList<>();
        ArrayList<Vector> listSMA50 = new ArrayList<>();

        //Create Historical Candles
        try {
            int candleAmount = (int) (getSize().getX() / 18) + 1;
            for (int i = 0; i < candleAmount; i++) {
                double x = getSize().getX() - (separation + candleWidth) * (i + 1);
                DateTime candleTime = new DateTime(endTime.getYearInt(), endTime.getMonthInt(), endTime.getDayInt(), endTime.getHourInt(), endTime.getMinuteInt(), endTime.getSecondInt() - stock.getTimeFrame().getSeconds() * i);
                CandleUI historicalCandle = new CandleUI(stock, candleTime, x, focusY, focusPrice, candleWidth, candleScale);
                listCandle.add(historicalCandle);
                double emaY = focusY -(SMA.getCloseValue(candleTime) * candleScale.getY()) + focusPrice*candleScale.getY();
                if (SMA.getCloseValue(candleTime) != -1) listSMA50.add(new Vector(x + candleWidth / 2,emaY));
            }
        } catch (NullPointerException e) {
        }

        //Draw Candles
        for (CandleUI candle : listCandle) {
            if (candle.getStock().getOpen(candle.getOpenTime()) != -1) {
                candle.draw();
                candle.tick(this); //Update Mouse Over
            }
        }

        LineUI line = new LineUI(ColorE.BLUE, LineUI.Type.PLAIN,4d,listSMA50.toArray(new Vector[0]));
        line.draw();
        //Draw Tooltips
        for (CandleUI candle : listCandle) {
            if (candle.getStock().getOpen(candle.getOpenTime()) != -1) {
                candle.tick(this); //Update Mouse Over
                if (candle.isMouseOver()) drawCandleTooltip(candle, getWindow().getScaledMousePos());
            }
        }

    }

    public static void drawCandleTooltip(CandleUI candle, Vector mousePos) {
        Stock stock = candle.getStock();
        int textSize = 10;
        double x = mousePos.getX() - 96;
        double y = mousePos.getY() - textSize * 5 - 7;

        //Bound X Left
        if (x < 0) {
            x = 0;
            mousePos = new Vector(60,mousePos.getY());
        }

        //Bound Y Up
        if (y < 0) {
            y = 0;
            mousePos = new Vector(mousePos.getX(),textSize * 4 + 5);
        }

        //Round Data
        double open = MathE.roundDouble(stock.getOpen(candle.getOpenTime()), 2);
        double close = MathE.roundDouble(stock.getClose(candle.getOpenTime()), 2);
        double min = MathE.roundDouble(stock.getMin(candle.getOpenTime()), 2);
        double max = MathE.roundDouble(stock.getMax(candle.getOpenTime()), 2);

        //Draw Background
        QuickDraw.drawRect(new Vector(x - 2, y), new Vector(mousePos.getX() - x + 2, mousePos.getY() - y - 1), new ColorE(0, 125, 200, 200));

        //Draw Data
        QuickDraw.drawText(candle.getOpenTime().getFormattedDateTime(),new Font("Arial", Font.PLAIN, textSize),new Vector(x,y),ColorE.WHITE);
        QuickDraw.drawText("Open:" + open, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize), ColorE.WHITE);
        QuickDraw.drawText("Close:" + close, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 2), ColorE.WHITE);
        QuickDraw.drawText("Min:" + min, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 3), ColorE.WHITE);
        QuickDraw.drawText("Max:" + max, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 4), ColorE.WHITE);

    }
}

