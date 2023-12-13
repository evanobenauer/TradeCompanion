package com.ejo.tradecompanion.scenes.experimental;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.math.VectorMod;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.SideBarUI;
import com.ejo.glowui.scene.elements.TextUI;
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
import com.ejo.tradecompanion.indicator.Indicator;
import com.ejo.tradecompanion.indicator.IndicatorEMA;
import com.ejo.tradecompanion.indicator.IndicatorSMA;
import com.ejo.tradecompanion.util.ProbabilityUtil;
import com.ejo.tradecompanion.util.RenderUtil;

import java.util.ArrayList;

public class BounceFinderScene extends Scene {

    private final Stock stock; //If the stock needs to be changed, switch to a new scene

    private final IndicatorEMA ema;
    private final IndicatorSMA sma;


    //TODO: Add toggle to update EMA/SMA live AND add a full calculation menu with date ranges
    private final ToggleUI toggleEMA = new ToggleUI("EMA", new Vector(30, 30), new Vector(100, 25), ColorE.BLUE, new Container<>(false));
    private final ToggleUI toggleSMA = new ToggleUI("SMA", new Vector(30, 30 + 30 + 5), new Vector(100, 25), ColorE.BLUE, new Container<>(false));

    private final SideBarUI sidebar = new SideBarUI(SideBarUI.Type.TOP, 120, true, ColorE.BLUE.alpha(120), toggleEMA, toggleSMA);

    private final ProgressBarUI<Double> progressBarClosePercent = new ProgressBarUI<Double>(Vector.NULL, new Vector(100, 30), ColorE.BLUE, new Container<>(0d), 0, 1);


    private DateTime chartStartTime;
    private final float startPrice;

    private int candleTimeOffset = 0;
    private double candlePriceOffset = 0;

    private final double defaultCandleWidth = 30;
    private final double defaultCandleSpace = 4;
    private final VectorMod candleScale = new VectorMod(1, 200);

    private boolean dragging = false;
    private DateTime dragTimeGrab;
    private VectorMod dragPosTemp;
    private Vector dragPosPre;
    private final VectorMod dragPos = Vector.NULL.getMod();

    private ArrayList<CandleUI> candleList = new ArrayList<>();


    public BounceFinderScene(Stock stock) {
        super("Chart Viewer");
        this.stock = stock;
        this.chartStartTime = new DateTime(2023, 11, 30, 9, 30);
        this.startPrice = getStock().getClose(chartStartTime);

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

    //TODO: Daily candles have issues rendering

    @Override
    public void draw() {
        progressBarClosePercent.setPos(new Vector(getSize().getX() - progressBarClosePercent.getSize().getX(), getSize().getY() - progressBarClosePercent.getSize().getY()));
        drawBackground(new ColorE(30, 30, 30));

        //TODO: Draw arrow indicator if the candle price is below or above the focusPrice and off screen

        DateTime time = chartStartTime.getAdded(candleTimeOffset * getStock().getTimeFrame().getSeconds());

        double focusY = getSize().getY() / 2;
        double focusPrice = candlePriceOffset + startPrice;

        ArrayList<Indicator> indicatorsList = new ArrayList<>();
        if (toggleEMA.getContainer().get()) indicatorsList.add(ema);
        if (toggleSMA.getContainer().get()) indicatorsList.add(sma);

        Indicator[] indicators = indicatorsList.toArray(new Indicator[0]);

        RenderUtil.drawCandlesFromData(candleList,indicators);
        super.draw();


        //Draw Vertical Candle Line
        for (CandleUI candle : candleList) {
            if (isHoveredHorizontally(candle,getWindow().getScaledMousePos())) {
                LineUI line = new LineUI(new Vector(candle.getPos().getX() + candle.getWidth()/2,0),new Vector(candle.getPos().getX() + candle.getWidth()/2,getSize().getY()),ColorE.WHITE, LineUI.Type.DOTTED,1);
                line.draw();
                QuickDraw.drawTextCentered(String.valueOf(candle.getOpenTime()),Fonts.getDefaultFont(24),Vector.NULL.getAdded(candle.getPos().getX() + candle.getWidth()/2,getSize().getY() - 12),Vector.NULL,ColorE.WHITE);
            }
        }

        //Draw Horizontal Price Line
        LineUI line = new LineUI(new Vector(0,getWindow().getScaledMousePos().getY()),new Vector(getSize().getX(),getWindow().getScaledMousePos().getY()),ColorE.WHITE, LineUI.Type.DOTTED,1);
        line.draw();
        double yPrice = (focusY - getWindow().getScaledMousePos().getY()) / candleScale.getY() + focusPrice;
        TextUI text = new TextUI(String.valueOf(MathE.roundDouble(yPrice,2)),Fonts.getDefaultFont(25),Vector.NULL,ColorE.WHITE);
        text.setPos(new Vector(getSize().getX() - text.getWidth() - 2,getWindow().getScaledMousePos().getY() - text.getHeight() / 2));
        text.draw();
        tempDrawDebugElements();

    }

    @Override
    public void tick() {
        super.tick();

        double focusY = getSize().getY() / 2;
        double focusPrice = candlePriceOffset + startPrice;
        this.candleList = getOnScreenCandles(this, getStock(), chartStartTime, (int) (getSize().getX() / (candleScale.getX() * (defaultCandleWidth + defaultCandleSpace))), focusPrice, focusY, defaultCandleSpace, defaultCandleWidth, candleScale);
        if (dragging) updateDrag();

        getStock().updateLiveData();
        if (dragging) updateDragPriceScale();
    }

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
        runScrollScaling(scroll);
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
        updateDragMouseToggle(button, action, mousePos);

        //TODO: Display a menu for each timeframe when a candle is clicked
        if (button == Mouse.BUTTON_LEFT.getId() && action == Mouse.ACTION_CLICK && Key.isControlDown()) {
            for (CandleUI candle : candleList) {
                if (isHoveredHorizontally(candle,mousePos)) {
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
        tempKeyDebug(key);
    }


    private void startStockUpdateThread() {
        Thread stockUpdateThread = new Thread(() -> {
            while (true) {
                stock.updateLivePrice(.5);
                if (stock.getOpenTime() != null) {
                    ema.calculateData(stock.getOpenTime());
                    sma.calculateData(stock.getOpenTime());
                }
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

    private void updateDragMouseToggle(int button, int action, Vector mousePos) {
        if (button != Mouse.BUTTON_LEFT.getId()) return;
        if (action == Mouse.ACTION_CLICK) {
            for (CandleUI candle : candleList) {
                if (isHoveredHorizontally(candle,mousePos)) {
                    dragTimeGrab = candle.getOpenTime(); //Maybe just make this set the specific candle object?
                    break;
                }
                dragTimeGrab = null;
            }
            candleScalePre = candleScale;
            dragPosPre = mousePos;
            dragging = true;
        }

        if (action == Mouse.ACTION_RELEASE) {
            dragging = false;
            dragPos.add(dragPosTemp);
        }
    }

    VectorMod candleScalePre;

    private void updateDragPriceScale() {
        if (!Key.isShiftDown()) return;
        VectorMod dragPosTemp = getWindow().getScaledMousePos().getSubtracted(dragPosPre).getMod();
        //dragPosTemp.scale(1 / (candleScale.getX() * (defaultCandleSpace + defaultCandleWidth)), 1 / candleScale.getY());
        if (candleScalePre.getY() + dragPosTemp.getY() > 0) candleScale.setY(candleScalePre.getY() + dragPosTemp.getY());
        dragPosPre = getWindow().getScaledMousePos();
        this.dragPosTemp = Vector.NULL.getMod();
    }

    //This is a little slow. Maybe find a faster way to jump the gap
    private void updateDrag() {
        if (Key.isShiftDown()) return;
        //DRAG Y POSITION
        dragPosTemp = getWindow().getScaledMousePos().getSubtracted(dragPosPre).getMod();
        dragPosTemp.scale(1 / (candleScale.getX() * (defaultCandleSpace + defaultCandleWidth)), 1 / candleScale.getY());
        candlePriceOffset = dragPosTemp.getY() + dragPos.getY();

        //DRAG X POSITION
        int grabIndex = 0;
        int tempIndex = getWindow().getScaledMousePos().getX() > getSize().getX() ? 0 : candleList.size() - 1;
        boolean foundHoveredCandle = false;
        boolean foundGrabCandle = false;
        for (CandleUI candle : candleList) {
            if (!foundGrabCandle && dragTimeGrab.equals(candle.getOpenTime())) {
                grabIndex = candleList.indexOf(candle);
                foundGrabCandle = true;
            }

            if (!foundHoveredCandle && isHoveredHorizontally(candle,getWindow().getScaledMousePos())) {
                tempIndex = candleList.indexOf(candle);
                foundHoveredCandle = true;
            }
            if (foundHoveredCandle && foundGrabCandle) break;
        }

        int indexDiff = grabIndex - tempIndex;
        int correction = indexDiff * getStock().getTimeFrame().getSeconds();
        int sign = correction != 0 ? Math.abs(correction) / correction : 0;

        //Jump the time gap when the price is not active.
        // This has issues because of gap jumping requiring multiple update iterations to fully complete.
        // MAYBE instead of the below, check if the day is different, then add more to the correction value if so.
        while (!StockUtil.isPriceActive(getStock().isExtendedHours(),chartStartTime.getAdded(-correction)))
            correction += sign * getStock().getTimeFrame().getSeconds();

        chartStartTime = chartStartTime.getAdded(-correction);
    }


    private void runScrollScaling(int scroll) {
        float min = .02f;
        float speed = (float) scroll / 100;
        if (Key.isShiftDown()) {
            candleScale.add(0,-speed * 200);
            if (candleScale.getY() < min * 200) candleScale.setY(min * 200);
        } else {
            candleScale.add(speed, 0);
            if (candleScale.getX() < min) candleScale.setX(min);
        }
    }


    public static ArrayList<CandleUI> getOnScreenCandles(Scene scene, Stock stock, DateTime lastTime, int candleCount, double focusPrice, double focusY, double separation, double candleWidth, Vector candleScale) {
        ArrayList<CandleUI> listCandle = new ArrayList<>();

        int currentCandles = 0;
        int loopCount = 0;
        while (currentCandles < candleCount) { //This is mildly inefficient. Maybe rewrite it someday ¯\_(ツ)_/¯
            DateTime candleTime = lastTime.getAdded(-loopCount * stock.getTimeFrame().getSeconds());

            if (!StockUtil.isPriceActive(stock.isExtendedHours(), candleTime)) {
                loopCount++;
                continue;
            }

            double x = scene.getSize().getX() - ((separation + candleWidth) * (currentCandles + 1)) * candleScale.getX();
            if (!(x + candleWidth < 0 || x > scene.getSize().getX())) {
                CandleUI candle = new CandleUI(stock, candleTime, x, focusY, focusPrice, candleWidth * candleScale.getX(), new Vector(1, candleScale.getY()));
                listCandle.add(candle);
            }

            currentCandles++;
            loopCount++;
        }

        return listCandle;
    }


    @Deprecated
    private void tempKeyDebug(int key) {
        if (!getWindow().isDebug()) return;

        if (key == Key.KEY_U.getId()) {
            System.out.println("Updating Data");
            AlphaVantageDownloader downloader = new AlphaVantageDownloader("H0JHAOU61I4MESDZ", false, getStock().getTicker(), getStock().getTimeFrame(), true);
            downloader.download(StockUtil.getAdjustedCurrentTime().getYear(), StockUtil.getAdjustedCurrentTime().getMonth());
            downloader.combineToLiveFile();
            getStock().applyHistoricalData();
            DateTime startCandle = getStock().getOpenTime();
            ema.calculateData(startCandle.getAdded(-30 * 24 * 60 * 60),startCandle);
            sma.calculateData(startCandle.getAdded(-30 * 24 * 60 * 60),startCandle);
        }

        if (key == Key.KEY_C.getId()) {
            Thread thread = new Thread(this::tempCalculateData);
            thread.setDaemon(true);
            thread.start();
        }
        if (key == Key.KEY_S.getId()) {
            Thread thread = new Thread(this::tempSaveData);
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Deprecated
    private void tempCalculateData() {
        DateTime startCandle = new DateTime(2023,12,12,19,59);
        //DateTime startCandle = new DateTime(2023, 12, 17, 19, 0);
        //DateTime startCandle = getStock().getOpenTime();
        ema.calculateData(startCandle.getAdded(-30 * 24 * 60 * 60), startCandle);
        sma.calculateData(startCandle.getAdded(-30 * 24 * 60 * 60),startCandle);
    }

    private void tempSaveData() {
        System.out.println("Saving Data");
        sma.saveHistoricalData();
        ema.saveHistoricalData();
        stock.saveHistoricalData();
        System.out.println("Data Saved!");
    }

    @Deprecated
    private void tempDrawDebugElements() {
        //Draw EMA Calculation Progress Bars
        if (getWindow().isDebug()) {
            if (stock.isProgressActive()) {
                ProgressBarUI<Double> progressBar = new ProgressBarUI<>(new Vector(2, getSize().getY() - 20), new Vector(200, 20), ColorE.BLUE, stock.getProgressContainer(), 0, 1);
                QuickDraw.drawText("[" + stock.getTicker() + "] Saving...", Fonts.getDefaultFont(20), new Vector(progressBar.getSize().getX() + 4, progressBar.getPos().getY()), ColorE.WHITE);
                progressBar.draw();
            }
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
        System.out.println("---");
        System.out.println("Starting Calculation for candle: " + candleTime);
        float precision = .03f; //Maybe try .02f
        int lookBackAmount = 4;
        int lookForwardAmount = 5;
        boolean priceScale = true;
        boolean ignoreWicks = true;
        boolean includeAfterHours = false;

        ArrayList<ArrayList<Long>> similarResultsList = new ArrayList<>();
        Container<float[]> results = new Container<>();

        similarResultsList.add(ProbabilityUtil.getSimilarCandleIDs(getStock(), candleTime, precision, priceScale, ignoreWicks, includeAfterHours, lookForwardAmount, results));
        System.out.println("---");
        System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in " + lookForwardAmount + " Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");

        for (int l = 1; l <= lookBackAmount; l++) {
            similarResultsList.add(ProbabilityUtil.filterSimilarCandlesFromPrevious(getStock(), candleTime, precision, priceScale, ignoreWicks, includeAfterHours, similarResultsList.get(l - 1), l, lookForwardAmount, results));
            System.out.println("---");
            System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in " + lookForwardAmount + " Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
        }
        return results;
    }

    private boolean isHoveredHorizontally(CandleUI candle, Vector mousePos) {
        return mousePos.getX() >= candle.getPos().getX() - defaultCandleSpace * candleScale.getX()/2 && mousePos.getX() <= candle.getPos().getX() + candle.getBodySize().getX() + defaultCandleSpace*candleScale.getX()/2;
    }


    public Stock getStock() {
        return stock;
    }

}
