package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.render.CandleUI;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.indicator.IndicatorSMA;

import java.awt.*;
import java.util.ArrayList;

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
        drawCandles(stock, StockUtil.getAdjustedCurrentTime(),focusPrice,focusY,candleSpace,candleWidth,candleScale);
    }

    @Override
    public void tick() {
        super.tick();
        stock.updateLiveData();
    }

    private void drawCandles(Stock stock, DateTime endTime, double focusPrice, double focusY, double separation, double candleWidth, Vector candleScale) {
        //Define Candle List
        ArrayList<CandleUI> listCandle = new ArrayList<>();
        ArrayList<Vector> listSMA50 = new ArrayList<>();

        DateTime ot = endTime.equals(StockUtil.getAdjustedCurrentTime()) ? stock.getOpenTime() : endTime;

        //Create Historical Candles
        try {
            int candleAmount = (int) (getSize().getX() / 18) + 1;
            for (int i = 0; i < candleAmount; i++) {
                double x = getSize().getX() - (separation + candleWidth) * (i + 1);
                DateTime candleTime = new DateTime(ot.getYear(), ot.getMonth(), ot.getDay(), ot.getHour(), ot.getMinute(), ot.getSecond() - stock.getTimeFrame().getSeconds() * i);
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

        try {
            LineUI line = new LineUI(ColorE.BLUE, LineUI.Type.PLAIN, 4d, listSMA50.toArray(new Vector[0]));
            line.draw();
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
