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
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.indicator.IndicatorEMA;
import com.ejo.tradecompanion.indicator.IndicatorSMA;

import java.awt.*;
import java.util.ArrayList;

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
        drawCandles(stock,time,focusPrice,getSize().getY() / 2 + candleYOffset,6 * candleScaleX,20,new Vector(candleScaleX,candleScaleY));
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

    private void drawCandles(Stock stock, DateTime endTime, double focusPrice, double focusY, double separation, double candleWidth, Vector candleScale) {
        //Define Candle List
        ArrayList<CandleUI> listCandle = new ArrayList<>();
        ArrayList<Vector> listSMA50 = new ArrayList<>();
        ArrayList<Vector> listEMA50 = new ArrayList<>();

        DateTime openTime = endTime.equals(StockUtil.getAdjustedCurrentTime()) ? stock.getOpenTime() : endTime;

        //Create Historical Candles
        try {
            int candleAmount = 100;//(int) (getSize().getX() / 18) + 1;
            for (int i = 0; i < candleAmount; i++) {
                double x = getSize().getX() - ((separation + candleWidth) * (i + 1)) * candleScale.getX();
                DateTime candleTime = new DateTime(openTime.getYear(), openTime.getMonth(), openTime.getDay(), openTime.getHour(), openTime.getMinute(), openTime.getSecond() - stock.getTimeFrame().getSeconds() * i);
                CandleUI historicalCandle = new CandleUI(stock, candleTime, x, focusY, focusPrice, candleWidth * candleScale.getX(), new Vector(1,candleScale.getY()));
                listCandle.add(historicalCandle);
                double smaY = focusY -(SMA.getCloseValue(candleTime) * candleScale.getY()) + focusPrice*candleScale.getY();
                if (SMA.getCloseValue(candleTime) != -1) listSMA50.add(new Vector(x + candleWidth / 2,smaY));

                double emaY = focusY -(EMA.getCloseValue(candleTime) * candleScale.getY()) + focusPrice*candleScale.getY();
                if (EMA.getCloseValue(candleTime) != -1) listEMA50.add(new Vector(x + candleWidth / 2,emaY));
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

        try {
            LineUI lineSMA = new LineUI(ColorE.BLUE, LineUI.Type.PLAIN, 4d, listSMA50.toArray(new Vector[0]));
            lineSMA.draw();
        } catch (Exception e) {
        }

        try {

            LineUI lineEMA = new LineUI(ColorE.YELLOW, LineUI.Type.PLAIN, 4d, listEMA50.toArray(new Vector[0]));
            lineEMA.draw();
        } catch (Exception e) {
        }

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
        QuickDraw.drawText(candle.getOpenTime().toString(),new Font("Arial", Font.PLAIN, textSize),new Vector(x,y),ColorE.WHITE);
        QuickDraw.drawText("Open:" + open, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize), ColorE.WHITE);
        QuickDraw.drawText("Close:" + close, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 2), ColorE.WHITE);
        QuickDraw.drawText("Min:" + min, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 3), ColorE.WHITE);
        QuickDraw.drawText("Max:" + max, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 4), ColorE.WHITE);

    }
}

