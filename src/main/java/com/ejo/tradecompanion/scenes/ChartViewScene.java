package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.math.VectorMod;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.SideBarUI;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.glowui.scene.elements.widget.ToggleUI;
import com.ejo.glowui.util.Key;
import com.ejo.glowui.util.Mouse;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.data.api.AlphaVantageDownloader;
import com.ejo.stockdownloader.render.CandleUI;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.indicator.Indicator;
import com.ejo.tradecompanion.indicator.IndicatorEMA;
import com.ejo.tradecompanion.indicator.IndicatorSMA;
import com.ejo.tradecompanion.util.ProbabilityUtil;
import com.ejo.tradecompanion.util.RenderUtil;

import java.util.ArrayList;

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
    private final VectorMod candleScale = new VectorMod(1, 200);

    private boolean dragging = false;
    private Vector dragPosPre = Vector.NULL;
    private final VectorMod dragPos = Vector.NULL.getMod();

    private ArrayList<CandleUI> candleList = new ArrayList<>();


    public ChartViewScene(Stock stock) {
        super("Chart Viewer");
        this.stock = stock;
        this.startTime = new DateTime(2023, 11, 30, 11, 0);
        this.startPrice = getStock().getClose(startTime);

        // ----------------- Temporary Data ------------------
        this.ema = new IndicatorEMA(stock, 50);
        this.sma = new IndicatorSMA(stock, 50);

        //ema.loadHistoricalData(); //TODO: make sure to not include loading in constructor in future
        sma.loadHistoricalData();

        progressBarClosePercent.setContainer(stock.getClosePercent());

        // --------------------------------------------------

        startStockUpdateThread();
        addElements(sidebar, progressBarClosePercent);
    }

    @Override
    public void draw() {
        progressBarClosePercent.setPos(new Vector(getSize().getX() - progressBarClosePercent.getSize().getX(), getSize().getY() - progressBarClosePercent.getSize().getY()));
        drawBackground(new ColorE(30, 30, 30));

        //TODO: Draw arrow indicator if the candle price is below or above the focusPrice and off screen

        DateTime time = startTime.getAdded(candleTimeOffset * getStock().getTimeFrame().getSeconds());

        double focusY = getSize().getY() / 2;
        double focusPrice = candlePriceOffset + startPrice;

        ArrayList<Indicator> indicatorsList = new ArrayList<>();
        if (toggleEMA.getContainer().get()) indicatorsList.add(ema);
        if (toggleSMA.getContainer().get()) indicatorsList.add(sma);

        Indicator[] indicators = indicatorsList.toArray(new Indicator[0]);
        this.candleList = RenderUtil.drawStockData(this, getStock(), time, (int) (getSize().getX() / (candleScale.getX() * (defaultCandleWidth + defaultCandleSpace))), focusPrice, focusY, defaultCandleSpace, defaultCandleWidth, candleScale, indicators);
        super.draw();


        for (CandleUI candle : candleList) {
            if (isHoveredHorizontally(candle,getWindow().getScaledMousePos())) {
                LineUI line = new LineUI(new Vector(candle.getPos().getX() + candle.getWidth()/2,0),new Vector(candle.getPos().getX() + candle.getWidth()/2,getSize().getY()),ColorE.WHITE, LineUI.Type.DOTTED,1);
                line.draw();
                QuickDraw.drawTextCentered(String.valueOf(candle.getOpenTime()),Fonts.getDefaultFont(24),Vector.NULL.getAdded(candle.getPos().getX() + candle.getWidth()/2,getSize().getY() - 12),Vector.NULL,ColorE.WHITE);
            }
        }

        tempDrawDebugElements();

    }

    @Override
    public void tick() {
        super.tick();
        getStock().updateLiveData();
        if (dragging) updateDrag();
    }

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
        runScrollScaling(scroll);
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
        updateDragToggle(button, action, mousePos);
        //TODO: Display a menu for each timeframe when a candle is clicked
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
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (action == Key.ACTION_RELEASE) return;
        if (key == Key.KEY_DOWN.getId()) candleScale.add(0, -2);
        if (key == Key.KEY_UP.getId()) candleScale.add(0, 2);

        tempKeyDebug(key);
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
            dragPosPre = mousePos;
            dragging = true;
        }

        if (action == Mouse.ACTION_RELEASE) dragging = false;
    }

    private void updateDrag() {
        VectorMod dragPosTemp = getWindow().getScaledMousePos().getSubtracted(dragPosPre).getMod();
        dragPosTemp.scale(1 / (candleScale.getX() * (defaultCandleSpace + defaultCandleWidth)), 1 / candleScale.getY());

        //Skip over inactive stock hours
        //TODO: Gap skipping has issues when you drag over multiple timeframes in the same tick.
        // This only matters when on bigger timeframes and massive scales.
        // Maybe this has something to do with weekends? Maybe not?
        DateTime mainTime = startTime.getAdded((int) -(dragPos.getX()) * getStock().getTimeFrame().getSeconds());
        DateTime tempTime = startTime.getAdded((int) -(dragPosTemp.getX() + dragPos.getX()) * getStock().getTimeFrame().getSeconds());
        if (!StockUtil.isPriceActive(getStock().isExtendedHours(), tempTime)) {
            int seconds = (8 * 60 * 60) / getStock().getTimeFrame().getSeconds();
            if (!getStock().isExtendedHours()) seconds = (int) (((getStock().getTimeFrame().getSeconds() < TimeFrame.ONE_HOUR.getSeconds() ? 17.5 : 18) * 60 * 60) / getStock().getTimeFrame().getSeconds());
            if (tempTime.isWeekend()) seconds = (48 * 60 * 60) / getStock().getTimeFrame().getSeconds();

            if (tempTime.getDateTimeID() > mainTime.getDateTimeID()) {
                dragPosTemp.subtract(seconds, 0);
            } else {
                dragPosTemp.add(seconds, 0);
            }
        }

        dragPos.add(dragPosTemp);

        candleTimeOffset = (int) -dragPos.getX();
        candlePriceOffset = dragPos.getY();

        dragPosPre = getWindow().getScaledMousePos();
    }

    private void runScrollScaling(int scroll) {
        float min = .02f;
        float speed = (float) scroll / 100;
        candleScale.add(speed, 0);
        if (candleScale.getX() < min) candleScale.setX(min);
    }


    @Deprecated
    private void tempKeyDebug(int key) {
        if (!getWindow().isDebug()) return;
        if (key == Key.KEY_U.getId()) {
            AlphaVantageDownloader downloader = new AlphaVantageDownloader("H0JHAOU61I4MESDZ", false, getStock().getTicker(), getStock().getTimeFrame(), true);
            downloader.download(StockUtil.getAdjustedCurrentTime().getYear(), StockUtil.getAdjustedCurrentTime().getMonth());
            downloader.combineToLiveFile();
            getStock().loadHistoricalData();
        }

        if (key == Key.KEY_C.getId()) {
            Thread thread = new Thread(() -> {
                //DateTime startCandle = new DateTime(2023,12,1,19,59);
                DateTime startCandle = new DateTime(2023, 12, 1, 19, 0);

                //ema.calculateData(startCandle.getAdded(-30 * 24 * 60 * 60),startCandle);
                //sma.calculateData(startCandle.getAdded(-30 * 24 * 60 * 60),startCandle);
                ema.calculateData(startCandle.getAdded(-2 * 12 * 31 * 24 * 60 * 60), startCandle);
                //sma.calculateData(startCandle.getAdded(-2 * 12 * 31 * 24 * 60 * 60), startCandle);
            });
            thread.setDaemon(true);
            thread.start();
        }
        if (key == Key.KEY_S.getId()) {
            sma.saveHistoricalData();
            ema.saveHistoricalData();
        }
    }

    @Deprecated
    private void tempDrawDebugElements() {
        if (getWindow().isDebug()) {
            if (sma.isProgressActive() && sma.getCurrentCalculationDate() != null) {
                ProgressBarUI<Double> progressBar = new ProgressBarUI<>(new Vector(2, getSize().getY() - 20), new Vector(200, 20), ColorE.BLUE, sma.getProgressContainer(), 0, 1);
                QuickDraw.drawText(sma.getCurrentCalculationDate().toString(), Fonts.getDefaultFont(20), new Vector(progressBar.getSize().getX() + 4, progressBar.getPos().getY()), ColorE.WHITE);
                progressBar.draw();
            }
            if (ema.isProgressActive() && ema.getCurrentCalculationDate() != null) {
                ProgressBarUI<Double> progressBar = new ProgressBarUI<>(new Vector(2, getSize().getY() - 20), new Vector(200, 20), ColorE.BLUE, ema.getProgressContainer(), 0, 1);
                QuickDraw.drawText(ema.getCurrentCalculationDate().toString(), Fonts.getDefaultFont(20), new Vector(progressBar.getSize().getX() + 4, progressBar.getPos().getY()), ColorE.WHITE);
                progressBar.draw();
            }
        }
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

        similarResultsList.add(ProbabilityUtil.getSimilarCandleIDs(getStock(), candleTime, precision, priceScale, ignoreWicks, includeAfterHours, results));
        System.out.println("---");
        System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in 3 Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");

        for (int l = 1; l <= lookBackAmount; l++) {
            similarResultsList.add(ProbabilityUtil.filterSimilarCandlesFromPrevious(getStock(), candleTime, precision, priceScale, ignoreWicks, includeAfterHours, similarResultsList.get(l - 1), l, results));
            System.out.println("---");
            System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in 3 Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
        }
        return results;
    }

    private boolean isHoveredHorizontally(CandleUI candle, Vector mousePos) {
        return mousePos.getX() >= candle.getPos().getX() && mousePos.getX() <= candle.getPos().getX() + candle.getBodySize().getX();
    }


    public Stock getStock() {
        return stock;
    }

}
