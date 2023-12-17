package com.ejo.tradecompanion.scenes.experimental;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.util.Key;
import com.ejo.glowui.util.Mouse;
import com.ejo.glowui.util.Util;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.elements.CandleUI;
import com.ejo.tradecompanion.util.ProbabilityUtil;
import com.ejo.tradecompanion.util.RenderUtil;
import com.ejo.tradecompanion.util.TimeFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This is an experimental scene for getting the probability function indicator to work
 */
public class ProbabilityScene extends Scene {

    Stock stock = new Stock("SPY", TimeFrame.ONE_MINUTE,true, Stock.PriceSource.MARKETWATCH);
    DateTime time = new DateTime(2023,9,21,11,4,0);

    TextUI text = new TextUI("", Fonts.getDefaultFont(30),Vector.NULL,ColorE.WHITE);

    public ProbabilityScene() {
        super("Probability Scene");
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
        addElements(text);
    }

    ArrayList<Long> similarCandles = new ArrayList<>();

    @Override
    public void draw() {
        if (stock.getOpenTime() != null) time = stock.getOpenTime().getAdded(-stock.getTimeFrame().getSeconds());

        drawBackground(new ColorE(50, 50, 50, 255));
        try {
            super.draw();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        //Draw Close Percent
        ProgressBarUI<Double> prog = new ProgressBarUI<>(new Vector(0,getSize().getY() - 50 - 25),new Vector(100,25),ColorE.BLUE,stock.getClosePercent(),0,1);

        QuickDraw.drawRect(prog.getPos(),prog.getSize(),ColorE.BLACK);
        prog.draw();

        //Draw Test Candle
        CandleUI candle = new CandleUI(stock,time,3,getSize().getY() / 2 + 100,stock.getOpen(time),30,new Vector(1,200));
        candle.tick(this);
        candle.draw(this);
        if (candle.isMouseOver()) {
            RenderUtil.drawCandleTooltip(candle,getWindow().getScaledMousePos());
            ArrayList<CandleUI> candList = new ArrayList<>();
            double pX = getSize().getX() / 2;
            double pY = getSize().getY() - 200;
            double focPrice  = stock.getOpen(time);
            //pre-candles
            for (int i = 0; i < 5; i++) {
                candList.add(new CandleUI(stock, time.getAdded(-stock.getTimeFrame().getSeconds()*i), pX - i*31, pY, focPrice, 30, new Vector(1, 400)));
            }
            //post-candles
            for (int i = 1; i < 4; i++) {
                candList.add(new CandleUI(stock, time.getAdded(stock.getTimeFrame().getSeconds()*i), pX + i*31, pY, focPrice, 30, new Vector(1, 400)));
            }

            QuickDraw.drawRect(candList.get(5).getPos().getSubtracted(new Vector(31,20)),new Vector(31,100),new ColorE(0,125,200,100));

            for (CandleUI candley: candList) {
                candley.draw(this);
                candley.tick(this);
            }
        }
        QuickDraw.drawText(candle.getOpenTime().toString(), Fonts.getDefaultFont(20),candle.getPos().getAdded(candle.getWidth(),0),ColorE.WHITE);

        //Draw Similar Candles
        double x = 3;
        for (Long id : similarCandles) {
            if (x > getSize().getX()) continue;
            DateTime time = new DateTime(id);
            CandleUI candleUI = new CandleUI(stock, time, x, getSize().getY() / 2, stock.getOpen(time), 30, new Vector(1, 500));
            if (x + 30 > 0) {
                candleUI.draw(this);
                candleUI.tick(this);
                if (candleUI.isMouseOver()) {
                    RenderUtil.drawCandleTooltip(candleUI, getWindow().getScaledMousePos());
                    ArrayList<CandleUI> candList = new ArrayList<>();
                    double pX = getSize().getX() / 2;
                    double pY = getSize().getY() - 200;
                    double focPrice  = stock.getOpen(time);
                    //pre-candles
                    for (int i = 0; i < 5; i++) {
                        candList.add(new CandleUI(stock, time.getAdded(-stock.getTimeFrame().getSeconds()*i), pX - i*31, pY, focPrice, 30, new Vector(1, 400)));
                    }
                    //post-candles
                    for (int i = 1; i < 4; i++) {
                        candList.add(new CandleUI(stock, time.getAdded(stock.getTimeFrame().getSeconds()*i), pX + i*31, pY, focPrice, 30, new Vector(1, 400)));
                    }

                    QuickDraw.drawRect(candList.get(5).getPos().getSubtracted(new Vector(31,20)),new Vector(31,100),new ColorE(0,125,200,100));

                    for (CandleUI candley: candList) {
                        candley.draw(this);
                        candley.tick(this);
                    }
                }
            }
            x += candleUI.getWidth() + 3;
        }

        QuickDraw.drawText(String.valueOf(wins),Fonts.getDefaultFont(20),new Vector(2,getSize().getY() - 44),ColorE.WHITE);
        QuickDraw.drawText(String.valueOf(losses),Fonts.getDefaultFont(20),new Vector(2,getSize().getY() - 22),ColorE.WHITE);
    }

    StopWatch watch = new StopWatch();

    @Override
    public void tick() {
        watch.start();
        getWindow().setEconomic(true);
        super.tick();
        stock.updateLiveData();
        if (watch.hasTimePassedS(.5f)) {
            Util.forceRenderFrame();
            watch.restart();
        }
    }

    int wins = 0;
    int losses = 0;

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
        if (action != Mouse.ACTION_CLICK) return;
        if (button == Mouse.BUTTON_LEFT.getId()) wins++;
        if (button == Mouse.BUTTON_RIGHT.getId()) losses++;
        if (button == Mouse.BUTTON_MIDDLE.getId()) wins = losses = 0;
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (action == Key.ACTION_RELEASE) return;

        int speed = 1;
        if (key == Key.KEY_RIGHT.getId()) time = time.getAdded(speed * stock.getTimeFrame().getSeconds());
        if (key == Key.KEY_LEFT.getId()) time = time.getAdded(-speed * stock.getTimeFrame().getSeconds());
        if (key == Key.KEY_DOWN.getId()) time = time.getAdded(-60*60*24);
        if (key == Key.KEY_UP.getId()) time = time.getAdded(60*60*24);

        if (key == Key.KEY_C.getId()) {
            Thread thread = new Thread(() -> {
                DateTime time = this.time;
                System.out.println("---");
                System.out.println("Starting Calculation for candle: " + time);
                float precision = .03f; //Maybe try .02f
                int lookBackAmount = 4;
                int lookForwardAmount = 5;
                boolean priceScale = true;
                boolean ignoreWicks = true;
                boolean includeAfterHours = false;

                ArrayList<ArrayList<Long>> similarResultsList = new ArrayList<>();
                Container<float[]> results = new Container<>();

                similarResultsList.add(ProbabilityUtil.getSimilarCandleIDs(stock, time, precision, priceScale, ignoreWicks, includeAfterHours, lookForwardAmount, results));
                System.out.println("---");
                System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in " + lookForwardAmount + " Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");

                for (int l = 1; l <= lookBackAmount; l++) {
                    similarResultsList.add(ProbabilityUtil.filterSimilarCandlesFromPrevious(stock, time, precision, priceScale, ignoreWicks, includeAfterHours, similarResultsList.get(l - 1), l, lookForwardAmount, results));
                    System.out.println("---");
                    System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in " + lookForwardAmount + " Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
                    text.setText(results.get()[0] + "\\nGreen Probability: " + results.get()[1] + "%\\nRed Probability: " + results.get()[2] + "%" + "\\nAvg change in 3 Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
                }
                this.similarCandles = similarResultsList.get(lookBackAmount);;
            });
            thread.setDaemon(true);
            thread.start();
        }

    }
}
