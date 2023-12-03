package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.math.VectorMod;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.SideBarUI;
import com.ejo.glowui.scene.elements.widget.ToggleUI;
import com.ejo.glowui.util.Key;
import com.ejo.glowui.util.Mouse;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.render.CandleUI;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.tradecompanion.indicator.Indicator;
import com.ejo.tradecompanion.indicator.IndicatorEMA;
import com.ejo.tradecompanion.indicator.IndicatorSMA;
import com.ejo.tradecompanion.util.ProbabilityUtil;
import com.ejo.tradecompanion.util.RenderUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class ChartViewScene extends Scene {

    private final Stock stock; //If the stock needs to be changed, switch to a new scene

    private final IndicatorEMA ema;
    private final IndicatorSMA sma;


    //TODO: Add toggle to update EMA/SMA live AND add a full calculation menu with date ranges
    private final ToggleUI toggleEMA = new ToggleUI("EMA", new Vector(30, 30), new Vector(100, 25), ColorE.BLUE, new Container<>(false));
    private final ToggleUI toggleSMA = new ToggleUI("SMA", new Vector(30, 30 + 30 + 5), new Vector(100, 25), ColorE.BLUE, new Container<>(false));

    private final SideBarUI sidebar = new SideBarUI(SideBarUI.Type.TOP, 120, true, ColorE.BLUE.alpha(120), toggleEMA, toggleSMA);

    private final ProgressBarUI<Double> progressBarClosePercent = new ProgressBarUI<Double>(Vector.NULL, new Vector(100, 30), ColorE.BLUE, new Container<>(0d), 0, 1);


    private final DateTime startTime;
    private final float startPrice;

    private int candleTimeOffset = 0;
    private double candlePriceOffset = 0;

    private final double defaultCandleWidth = 30;
    private final double defaultCandleSpace = 4;
    private final VectorMod candleScale = new VectorMod(1,200);

    private boolean dragging = false;
    private Vector dragPosStart = Vector.NULL;
    private VectorMod dragPosTemp = Vector.NULL.getMod();
    private final VectorMod dragPosMain = Vector.NULL.getMod();

    private ArrayList<CandleUI> candleList = new ArrayList<>();


    public ChartViewScene(Stock stock) {
        super("Chart Viewer");
        this.stock = stock;
        this.startTime = new DateTime(2023, 11, 30, 11, 30);
        this.startPrice = getStock().getClose(startTime);

        // ----------------- Temporary Data ------------------
        this.ema = new IndicatorEMA(stock, 50);
        this.sma = new IndicatorSMA(stock, 50);

        ema.loadHistoricalData(); //TODO: make sure to not include loading in constructor in future
        sma.loadHistoricalData();

        progressBarClosePercent.setContainer(stock.getClosePercent());

        // --------------------------------------------------

        startStockUpdateThread();
        addElements(sidebar, progressBarClosePercent);
    }

    @Override
    public void draw() {
        progressBarClosePercent.setPos(new Vector(0, getSize().getY() - 30));
        drawBackground(new ColorE(30, 30, 30));

        //TODO: Draw indicator if the candle price is below or above the focusPrice and off screen

        DateTime time = startTime.getAdded(candleTimeOffset * getStock().getTimeFrame().getSeconds());

        double focusY = getSize().getY() / 2;
        double focusPrice = candlePriceOffset + startPrice;

        ArrayList<Indicator> indicatorsList = new ArrayList<>();
        if (toggleEMA.getContainer().get()) indicatorsList.add(ema);
        if (toggleSMA.getContainer().get()) indicatorsList.add(sma);

        Indicator[] indicators = indicatorsList.toArray(new Indicator[0]);
        this.candleList = RenderUtil.drawStockData(this, getStock(), time, (int)(getSize().getX() / (candleScale.getX() * (defaultCandleWidth + defaultCandleSpace))), focusPrice, focusY, defaultCandleSpace, defaultCandleWidth, candleScale, indicators);
        super.draw();
    }

    @Override
    public void tick() {
        super.tick();
        getStock().updateLiveData();
        if (dragging) updateDrag();
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
        updateDragToggle(button,action,mousePos);
        if (button == Mouse.BUTTON_LEFT.getId() && action == Mouse.ACTION_CLICK && Key.isShiftDown()) {
            for (CandleUI candle : candleList) {
                if (candle.isMouseOver()) {
                    Thread thread = new Thread(() -> tempCalculateProbability(candle.getOpenTime()));
                    thread.setDaemon(true);
                    thread.start();
                }
            }
        }
    }

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
        runScrollScaling(scroll);
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (key == Key.KEY_DOWN.getId()) candleScale.add(0,-2);
        if (key == Key.KEY_UP.getId()) candleScale.add(0,2);
    }


    private void startStockUpdateThread() {
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

    private void updateDragToggle(int button, int action, Vector mousePos) {
        if (button != Mouse.BUTTON_LEFT.getId()) return;
        if (action == Mouse.ACTION_CLICK) {
            dragPosStart = mousePos;
            dragging = true;
        }

        if (action == Mouse.ACTION_RELEASE) {
            dragging = false;
            dragPosMain.add(dragPosTemp);
        }
    }

    private void updateDrag() {
        dragPosTemp = getWindow().getScaledMousePos().getSubtracted(dragPosStart).getMod();
        dragPosTemp.scale(1 / (candleScale.getX() * (defaultCandleSpace + defaultCandleWidth)), 1 / candleScale.getY());

        //Time Skip over Close Gap
        // Current Issues:
        // When scale is very low, there is sometimes a skip when crossing days. The skip happens in factors of 8 hours. so it has to do something with the datetimeID difference not being proper
        // ^ I think the skip happens when you drag your mouse over a separate date line than that which dragPosTemp is dealing with. I think its doing a second skip when crossing twice
        // Weekends do not currently skip
        DateTime mainTime = startTime.getAdded((int) -(dragPosMain.getX()) * getStock().getTimeFrame().getSeconds());
        DateTime tempTime = startTime.getAdded((int) -(dragPosTemp.getX() + dragPosMain.getX()) * getStock().getTimeFrame().getSeconds());

        if (!StockUtil.isPriceActive(getStock().isExtendedHours(), tempTime)) {
            int seconds = (8 * 60 * 60) / getStock().getTimeFrame().getSeconds();

            if (tempTime.getDateTimeID() > mainTime.getDateTimeID()) {
                dragPosTemp.subtract(seconds, 0);
            } else {
                dragPosTemp.add(seconds, 0);
            }
        }

        candleTimeOffset = (int) -(dragPosTemp.getX() + dragPosMain.getX());
        candlePriceOffset = dragPosTemp.getY() + dragPosMain.getY();
    }

    private void runScrollScaling(int scroll) {
        float min = .02f;
        float speed = (float) scroll / 100;
        candleScale.add(speed,0);
        if (candleScale.getX() < min) candleScale.setX(min);
    }


    @Deprecated
    private Container<float[]> tempCalculateProbability(DateTime candleTime) {
        System.out.println("Starting Calculation");
        float precision = .03f; //Maybe try .02f
        int lookBackAmount = 4;
        boolean priceScale = true;
        boolean ignoreWicks = true;
        boolean includeAfterHours = false;

        ArrayList<ArrayList<Long>> similarResultsList = new ArrayList<>();
        Container<float[]> results = new Container<>();

        similarResultsList.add(ProbabilityUtil.getSimilarCandleIDs(getStock(), candleTime, precision, priceScale,ignoreWicks,includeAfterHours,results));
        System.out.println("---");
        System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in 3 Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");

        for (int l = 1; l <= lookBackAmount; l++) {
            similarResultsList.add(ProbabilityUtil.filterSimilarCandlesFromPrevious(getStock(), candleTime, precision, priceScale, ignoreWicks,includeAfterHours,similarResultsList.get(l - 1), l,results));
            System.out.println("---");
            System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in 3 Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
        }
        return results;
    }


    public Stock getStock() {
        return stock;
    }

}
