package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.math.VectorMod;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.setting.Setting;
import com.ejo.glowlib.setting.SettingManager;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.util.ColorUtil;
import com.ejo.glowlib.util.TimeUtil;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.SideBarUI;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.scene.elements.shape.GradientRectangleUI;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.ModeCycleUI;
import com.ejo.glowui.scene.elements.widget.SliderUI;
import com.ejo.glowui.scene.elements.widget.TextFieldUI;
import com.ejo.glowui.util.input.Key;
import com.ejo.glowui.util.input.Mouse;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.App;
import com.ejo.stockdownloader.data.api.AlphaVantageDownloader;
import com.ejo.stockdownloader.util.DownloadTimeFrame;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.data.indicator.*;
import com.ejo.tradecompanion.elements.CandleUI;
import com.ejo.tradecompanion.elements.ListDisplayUI;
import com.ejo.tradecompanion.util.*;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

public class ChartViewScene extends Scene {

    //TODO: Clean this class code. Make more organized and modular with methods

    //Define Stock (If the stock needs to be changed, switch to a new scene)
    private final Stock stock;

    //Value Lists
    private ArrayList<CandleUI> candleList = new ArrayList<>();
    private final ArrayList<Indicator> listIndicator = new ArrayList<>();

    //Define Settings
    private final SettingManager settingManager;

    private final Setting<DateTime> chartTime;
    private final Setting<Float> chartFocusPrice;
    private final Setting<Vector> candleScale;

    //Saving Variables
    private boolean isSaving = false;

    //Candle Dimensions
    private final double candleWidth;
    private final double candleSeparation;

    //Dragging Variables
    private boolean dragging = false;
    private final VectorMod dragPos = Vector.NULL.getMod();
    private VectorMod dragPosTemp;
    private Vector dragPosPre;
    private final float focusPricePre;
    private Vector candleScalePre;
    private DateTime dragTimeGrab;


    //Indicator Bar
    private double yVal = 100;

    private final ModeCycleUI<String> modeCycleIndicator;
    private final ListDisplayUI<Indicator> listDisplayIndicator;
    private final ButtonUI buttonAddIndicator;

    //MA
    private final Vector optionSize = new Vector(100, 30);
    private final TextFieldUI fieldMAPeriod = new TextFieldUI(Vector.NULL, optionSize, ColorE.WHITE, new Container<>(""), "Period", true);
    private final ModeCycleUI<IndicatorMA.Type> modeMAType = new ModeCycleUI<>(Vector.NULL, optionSize, ChartUtil.WIDGET_COLOR, new Container<>(IndicatorMA.Type.CLOSE), IndicatorMA.Type.values());
    private final ModeCycleUI<String> modeMAColor = new ModeCycleUI<>(Vector.NULL, optionSize, ChartUtil.WIDGET_COLOR, new Container<>("Yellow"), "Red", "Orange", "Yellow", "Green", "Blue", "Purple", "White", "Black");
    private final SliderUI<Integer> sliderMAWidth = new SliderUI<>("Width", Vector.NULL, optionSize, ChartUtil.WIDGET_COLOR, new Container<>(1), 1, 10, 1, SliderUI.Type.INTEGER, true);

    private final SideBarUI indicatorBar = new SideBarUI(SideBarUI.Type.LEFT, 160, true, ChartUtil.WIDGET_COLOR.alpha(120),
            new TextUI("Add Indicator", Fonts.getDefaultFont(20), new Vector(22, yVal), ColorE.WHITE)
            , modeCycleIndicator = new ModeCycleUI<>(new Vector(20, yVal += 30), new Vector(120, 30), ChartUtil.WIDGET_COLOR, new Container<>("SMA"), "SMA", "EMA", "Probability", "MACD")
            , listDisplayIndicator = new ListDisplayUI<>(new Vector(80, 0), listIndicator).setFontSize(30)

            , buttonAddIndicator = new ButtonUI("Add", new Vector(20, yVal += 50), new Vector(120, 30), ChartUtil.WIDGET_COLOR, ButtonUI.MouseButton.LEFT, () -> {
        for (Indicator i : listDisplayIndicator.getList()) if (i.isProgressActive()) return;
        Indicator indicator = switch (modeCycleIndicator.getContainer().get()) {
            case "SMA" -> new IndicatorSMA(getStock(), Integer.parseInt(fieldMAPeriod.getContainer().get()), modeMAType.getContainer().get(), ColorUtil.getColorFromString(modeMAColor.getContainer().get()), sliderMAWidth.getContainer().get());
            case "EMA" -> new IndicatorEMA(getStock(), Integer.parseInt(fieldMAPeriod.getContainer().get()), modeMAType.getContainer().get(), ColorUtil.getColorFromString(modeMAColor.getContainer().get()), sliderMAWidth.getContainer().get());
            case "MACD" -> new IndicatorMACD(getStock(), 12, 26, 9, false);
            case "Probability" -> new IndicatorProbability(getStock(), .03f, 3, 5, false, true, false);
            default -> null;
        };
        assert indicator != null;
        for (Indicator i : listDisplayIndicator.getList()) {
            if (i.toString().equals(indicator.toString())) return;
        }
        listIndicator.add(indicator);
        Thread thread = new Thread(indicator::loadHistoricalData);
        thread.setDaemon(true);
        thread.start();
    })
            //Add Indicator Options
            , fieldMAPeriod
            , modeMAType
            , modeMAColor
            , sliderMAWidth
    );


    //Top Bar TODO: Add Color Font Selection, Jump-To-Date, Drag Speed?, Tooltips?
    private final ButtonUI indicatorButton = new ButtonUI("Indicators", new Vector(20, 25), new Vector(120, 30), ChartUtil.WIDGET_COLOR, ButtonUI.MouseButton.LEFT, () -> indicatorBar.setOpen(!indicatorBar.isOpen()));

    private final SideBarUI topBar = new SideBarUI(SideBarUI.Type.TOP, 80, true, new ColorE(0, 100, 100),
            indicatorButton
            //TODO: Add "Calculations" Sidebar menu with date ranges to calculate indicator data
    );


    public ChartViewScene(Stock stock) {
        super("Chart Viewer");
        this.stock = stock;
        this.settingManager = new SettingManager("setting", getStock().getTicker() + "-settings");

        //Set Default Setting Values
        DateTime ct = TimeUtil.getAdjustedCurrentTime();
        if (getStock().getTimeFrame().getSeconds() < TimeFrame.ONE_HOUR.getSeconds()) {
            this.chartTime = new Setting<>(settingManager, "chartTime", new DateTime(ct.getYear(), ct.getMonth(), 30, 9, 30));
        } else if (getStock().getTimeFrame().getSeconds() < TimeFrame.ONE_DAY.getSeconds()) {
            this.chartTime = new Setting<>(settingManager, "chartTime", new DateTime(ct.getYear(), ct.getMonth(), 30, 10, 0));
        } else {
            this.chartTime = new Setting<>(settingManager, "chartTime", new DateTime(ct.getYear(), ct.getMonth(), 30, 9, 30));
        }
        this.chartFocusPrice = new Setting<>(settingManager, "chartFocusPrice", getStock().getClose(chartTime.get()));
        this.candleScale = new Setting<>(settingManager, "chartCandleScale", new VectorMod(1, 200));

        //Load Settings
        settingManager.loadAll();

        //Set Time for Non-Extended Hours
        if (!StockUtil.shouldClose(chartTime.get(),stock.getTimeFrame())) {
            chartTime.set(new DateTime(chartTime.get().getYear(),chartTime.get().getMonth(),chartTime.get().getDay(),9,30));
            if (getStock().getTimeFrame().getSeconds() >= TimeFrame.ONE_HOUR.getSeconds() && getStock().getTimeFrame().getSeconds() < TimeFrame.ONE_DAY.getSeconds()) {
                chartTime.set(new DateTime(chartTime.get().getYear(),chartTime.get().getMonth(),chartTime.get().getDay(),10,0));
            }
        }

        int loopCount = 0;
        DateTime adjustedTime = chartTime.get();

        while (!StockUtil.isPriceActive(getStock().isExtendedHours(), chartTime.get().getAdded(-loopCount * stock.getTimeFrame().getSeconds()))) { //This is mildly inefficient. Maybe rewrite it someday ¯\_(ツ)_/¯
            loopCount++;
            adjustedTime = chartTime.get().getAdded(-loopCount * stock.getTimeFrame().getSeconds());
        }
        chartTime.set(adjustedTime);

        //Set Default Values
        this.focusPricePre = chartFocusPrice.get();
        this.candleWidth = 30;
        this.candleSeparation = 4;

        //TODO: find a way to save and load the indicator list. Or don't it isn't really necessary ¯\_(ツ)_/¯
        // Maybe do this outside the chart view so that you can have a progressbar on the title screen
        for (Indicator indicator : listIndicator) indicator.loadHistoricalData();

        indicatorBar.setOpen(false);
        indicatorBar.getButton().setEnabled(false);

        addElements(indicatorBar, topBar);

        runStockUpdateThread();
    }

    @Override
    public void draw() {
        //Draw Gradient Background
        new GradientRectangleUI(Vector.NULL, getSize(), new ColorE(0, 255, 255).alpha(20), new ColorE(0, 0, 0), GradientRectangleUI.Type.VERTICAL).draw();

        //Draw Candles & Indicators
        RenderUtil.drawAllData(this, candleList, listIndicator.toArray(new Indicator[0]));

        //Draw Close Percent Progress Bar
        ProgressBarUI<Double> progressBarClosePercent = new ProgressBarUI<>(Vector.NULL, new Vector(100, 20), ColorE.BLUE, getStock().getClosePercent(), 0, 1);
        progressBarClosePercent.setPos(new Vector(getSize().getX() - progressBarClosePercent.getSize().getX(), getSize().getY() - progressBarClosePercent.getSize().getY()));
        progressBarClosePercent.draw();

        updateIndicatorOptions();

        super.draw();

        drawCrossHair();

        for (Indicator indicator : listIndicator) {
            if (indicator.isProgressActive() && !isSaving)
                RenderUtil.drawProgressWheel(0, indicator.getProgressContainer().get(), 1, 20, getWindow().getScaledMousePos());
        }
    }

    @Override
    public void tick() {
        super.tick();
        getStock().updateLiveData();

        this.candleList = ChartUtil.getOnScreenCandles(this, getStock(), chartTime.get(), (int) (getSize().getX() / (candleScale.get().getX() * (candleWidth + candleSeparation))), chartFocusPrice.get(), getSize().getY() / 2, candleSeparation, candleWidth, candleScale.get());

        if (dragging) {
            updateDragPosition();
            updateDragPriceScale();
        }

        if (!topBar.isOpen()) indicatorBar.setOpen(false);

        listDisplayIndicator.setPos(new Vector(listDisplayIndicator.getPos().getX(), getSize().getY() - listDisplayIndicator.getHeight() - 2));
    }

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
        updateScrollScaling(scroll);
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
        updateDragMouseToggle(button, action, mousePos);

        //TODO: Display a menu for each timeframe when a candle is clicked

        tempDebugMouse(button, action, mods, mousePos);
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (action == Key.ACTION_RELEASE) return;
        if (key == Key.KEY_ESC.getId()) getWindow().setScene(new TitleScene());
        tempDebugKey(key);
    }


    private void runStockUpdateThread() {
        Thread stockUpdateThread = new Thread(() -> {
            while (getWindow().getScene().equals(this)) {
                //getStock().updateLivePrice(.5f); //Updates every half second tend to cause an ip ban on error 403
                getStock().updateLivePrice(1);
                try {
                    if (getStock().getOpenTime() != null) {
                        for (Indicator indicator : listIndicator) {
                            if (indicator instanceof IndicatorProbability prob) {
                                DateTime probCalculationTime = getStock().getOpenTime().getAdded(-getStock().getTimeFrame().getSeconds());
                                if (!prob.isCalculating() && getStock().shouldClose())
                                    new Thread(() -> prob.calculateData(probCalculationTime)).start();
                            } else {
                                indicator.calculateData(getStock().getOpenTime());
                            }
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    e.printStackTrace();
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

    private void drawCrossHair() {
        if (!(!topBar.isMouseOver() && !indicatorBar.isMouseOver())) return;
        //Draw Vertical Candle Line
        for (CandleUI candle : candleList) {
            if (ChartUtil.isHoveredHorizontally(candle, candleSeparation, getWindow().getScaledMousePos())) {
                double x = candle.getPos().getX() + candle.getWidth() * candle.getScale().getX() / 2;
                LineUI line = new LineUI(new Vector(x, 0), new Vector(x, getSize().getY()), ColorE.WHITE, LineUI.Type.DOTTED, 1);
                line.draw();
                QuickDraw.drawTextCentered(String.valueOf(candle.getOpenTime()), Fonts.getDefaultFont(24), Vector.NULL.getAdded(candle.getPos().getX() + candle.getWidth() / 2, getSize().getY() - 12), Vector.NULL, ColorE.WHITE);
                break;
            }
        }

        //Draw Horizontal Price Line
        LineUI line = new LineUI(new Vector(0, getWindow().getScaledMousePos().getY()), new Vector(getSize().getX(), getWindow().getScaledMousePos().getY()), ColorE.WHITE, LineUI.Type.DOTTED, 1);
        line.draw();
        double yPrice = (getSize().getY() / 2 - getWindow().getScaledMousePos().getY()) / candleScale.get().getY() + chartFocusPrice.get();
        TextUI text = new TextUI(String.valueOf(MathE.roundDouble(yPrice, 2)), Fonts.getDefaultFont(25), Vector.NULL, ColorE.WHITE);
        text.setPos(new Vector(getSize().getX() - text.getWidth() - 2, getWindow().getScaledMousePos().getY() - text.getHeight() / 2));
        text.draw();

        tempDebugDraw();
    }

    private void updateDragMouseToggle(int button, int action, Vector mousePos) {
        boolean canDrag = !(topBar.isMouseOver() || indicatorBar.isMouseOver());
        if (action == Mouse.ACTION_CLICK) {
            if (!canDrag) return;
            this.candleScalePre = candleScale.get();
            this.dragPosPre = mousePos;
            this.dragTimeGrab = null;
            for (CandleUI candle : candleList) {
                if (ChartUtil.isHoveredHorizontally(candle, candleSeparation, mousePos)) {
                    this.dragTimeGrab = candle.getOpenTime(); //Maybe just make this set the specific candle object?
                    break;
                }
            }

            this.dragging = true;
        }

        if (action == Mouse.ACTION_RELEASE) {
            this.dragging = false;
            if (canDrag) this.dragPos.add(dragPosTemp);
        }
    }

    private void updateDragPosition() {
        if (Mouse.BUTTON_RIGHT.isButtonDown()) return;

        //DRAG Y POSITION
        dragPosTemp = getWindow().getScaledMousePos().getSubtracted(dragPosPre).getMod();
        dragPosTemp.scale(1 / (candleScale.get().getX() * (candleSeparation + candleWidth)), 1 / candleScale.get().getY());
        chartFocusPrice.set(focusPricePre + (float) (dragPosTemp.getY() + dragPos.getY()));

        //DRAG X POSITION
        int grabIndex = 0;
        int tempIndex = getWindow().getScaledMousePos().getX() > getSize().getX() ? 0 : candleList.size() - 1;
        boolean foundHoveredCandle = false;
        boolean foundGrabCandle = false;
        for (CandleUI candle : candleList) {
            if (!foundGrabCandle && dragTimeGrab != null && dragTimeGrab.equals(candle.getOpenTime())) {
                grabIndex = candleList.indexOf(candle);
                foundGrabCandle = true;
            }

            if (!foundHoveredCandle && ChartUtil.isHoveredHorizontally(candle, candleSeparation, getWindow().getScaledMousePos())) {
                tempIndex = candleList.indexOf(candle);
                foundHoveredCandle = true;
            }
            if (foundHoveredCandle && foundGrabCandle) break;
        }

        int indexDiff = grabIndex - tempIndex;
        int correction = indexDiff * getStock().getTimeFrame().getSeconds();
        int sign = correction != 0 ? Math.abs(correction) / correction : 0;

        //This is a little slow. Maybe find a faster way to jump the gap
        //Jump the time gap when the price is not active.
        // This has issues because of gap jumping requiring multiple update iterations to fully complete.
        // MAYBE instead of the below, check if the day is different, then add more to the correction value if so.

        while (!StockUtil.isPriceActive(getStock().isExtendedHours(), chartTime.get().getAdded(-correction))) {
            correction += sign * getStock().getTimeFrame().getSeconds();
        }

        chartTime.set(chartTime.get().getAdded(-correction));
    }

    private void updateDragPriceScale() {
        if (!(Mouse.BUTTON_RIGHT.isButtonDown())) return;
        VectorMod dragPosTemp = getWindow().getScaledMousePos().getSubtracted(dragPosPre).getMod();
        //TODO: Make the multiplier based off of the hovered price?

        if (candleScalePre.getY() + dragPosTemp.getY() >= 0)
            candleScale.set(new Vector(candleScale.get().getX(), candleScalePre.getY() + dragPosTemp.getY()));

        this.dragPosTemp = Vector.NULL.getMod();
    }


    private void updateScrollScaling(int scroll) {
        float xMin = .02f;
        float yMin = 0f;
        float xMax = 1;
        float speed = (float) scroll / 100;
        if (Key.isShiftDown()) { //Y Scaling
            int mul = 100 * 3; //TODO: Make the multiplier based off of the hovered price?
            candleScale.set(candleScale.get().getAdded(0, speed * mul));
            if (candleScale.get().getY() < yMin) candleScale.set(new Vector(candleScale.get().getX(), yMin));
        } else { //X Scaling
            candleScale.set(candleScale.get().getAdded(speed, 0));
            if (candleScale.get().getX() < xMin) candleScale.set(new Vector(xMin, candleScale.get().getY()));
            if (candleScale.get().getX() > xMax) candleScale.set(new Vector(xMax, candleScale.get().getY()));
        }
    }

    private void updateIndicatorOptions() {
        fieldMAPeriod.setEnabled(false);
        modeMAType.setEnabled(false);
        modeMAColor.setEnabled(false);
        sliderMAWidth.setEnabled(false);

        switch (modeCycleIndicator.getContainer().get()) {
            case "SMA", "EMA" -> {
                int inc = 50;
                int startX = 30;
                fieldMAPeriod.setPos(new Vector(startX, yVal + inc));
                inc += 50;
                modeMAType.setPos(new Vector(startX, yVal + inc));
                inc += 50;
                modeMAColor.setPos(new Vector(startX, yVal + inc));
                inc += 50;
                sliderMAWidth.setPos(new Vector(startX, yVal + inc));

                fieldMAPeriod.setEnabled(true);
                modeMAType.setEnabled(true);
                modeMAColor.setEnabled(true);
                sliderMAWidth.setEnabled(true);
            }
            case "MACD" -> {
            }
            case "Probability" -> {
            }
        }
    }


    // -------------------------------TEMPORARY ELEMENTS START-----------------------------------------------

    @Deprecated
    private void tempDebugDraw() {
        //Draw EMA Calculation Progress Bars
        if (getWindow().isDebug()) {
            if (getStock().isProgressActive()) {
                ProgressBarUI<Double> progressBar = new ProgressBarUI<>(new Vector(2, getSize().getY() - 20), new Vector(200, 20), ColorE.BLUE, getStock().getProgressContainer(), 0, 1);
                QuickDraw.drawText("[" + getStock().getTicker() + "] Saving...", Fonts.getDefaultFont(20), new Vector(progressBar.getSize().getX() + 4, progressBar.getPos().getY()), ColorE.WHITE);
                progressBar.draw();
            }
            for (Indicator indicator : listIndicator) {
                if (indicator.isProgressActive()) {
                    ProgressBarUI<Double> progressBar = new ProgressBarUI<>(new Vector(2, getSize().getY() - 20), new Vector(200, 20), ColorE.BLUE, indicator.getProgressContainer(), 0, 1);
                    progressBar.draw();
                }
            }
        }
    }

    @Deprecated
    private void tempDebugMouse(int button, int action, int mods, Vector mousePos) {
        if (button == Mouse.BUTTON_LEFT.getId() && action == Mouse.ACTION_CLICK && Key.isControlDown()) {
            for (CandleUI candle : candleList) {
                if (ChartUtil.isHoveredHorizontally(candle, candleSeparation, mousePos)) {
                    Thread thread = new Thread(() -> tempCalculateProbability(candle.getOpenTime()));
                    thread.setDaemon(true);
                    thread.start();
                    break;
                }
            }
        }
    }

    @Deprecated
    private void tempDebugKey(int key) {
        if (!getWindow().isDebug()) return;

        if (key == Key.KEY_U.getId()) {
            System.out.println("Updating Data");
            AlphaVantageDownloader downloader = new AlphaVantageDownloader("H0JHAOU61I4MESDZ", false, getStock().getTicker(), DownloadTimeFrame.getFromTag(getStock().getTimeFrame().getTag()), true);
            downloader.download(TimeUtil.getAdjustedCurrentTime().getYear(), TimeUtil.getAdjustedCurrentTime().getMonth());
            downloader.combineToLiveFile();
            getStock().applyLoadHistoricalData();
            DateTime startCandle = getStock().getOpenTime();
            for (Indicator indicator : listIndicator)
                indicator.calculateData(startCandle.getAdded(-30 * 24 * 60 * 60), startCandle);
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
        DateTime endCandle = TimeUtil.getAdjustedCurrentTime();
        DateTime startCandle = TimeUtil.getAdjustedCurrentTime();

        for (CandleUI candle : candleList) {
            if (ChartUtil.isHoveredHorizontally(candle, candleSeparation, getWindow().getScaledMousePos())) {
                startCandle = candle.getOpenTime();
                break;
            }
        }

        for (Indicator indicator : listIndicator) {
            if (indicator instanceof IndicatorProbability) continue;
            indicator.calculateData(startCandle, endCandle);
        }
    }

    @Deprecated
    private void tempSaveData() {
        System.out.println("Saving Data");
        this.isSaving = true;
        for (Indicator indicator : listIndicator) {
            if (indicator instanceof IndicatorProbability) continue;
            indicator.applySaveHistoricalData();
        }
        getStock().applySaveHistoricalData();
        settingManager.saveAll();
        this.isSaving = false;
        System.out.println("Data Saved!");
    }

    @Deprecated
    private Container<float[]> tempCalculateProbability(DateTime candleTime) {
        System.out.println("---");
        System.out.println("Starting Calculation for candle: " + candleTime);
        float precision = .03f; //Maybe try .02f?
        int lookBackAmount = 4;
        int lookForwardAmount = 5;
        boolean priceScale = false;
        boolean ignoreWicks = true;
        boolean includeAfterHours = false;

        ArrayList<ArrayList<Long>> similarResultsList = new ArrayList<>();
        Container<float[]> results = new Container<>();

        similarResultsList.add(ProbabilityUtil.getSimilarCandleIDs(getStock(), candleTime, precision, priceScale, ignoreWicks, includeAfterHours, lookForwardAmount, results));
        System.out.println("---");
        System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in " + lookForwardAmount + " Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
        System.out.println("Run Time: " + results.get()[6] + "s");

        for (int l = 1; l <= lookBackAmount; l++) {
            similarResultsList.add(ProbabilityUtil.filterSimilarCandlesFromPrevious(getStock(), candleTime, precision, priceScale, ignoreWicks, includeAfterHours, similarResultsList.get(l - 1), l, lookForwardAmount, results));
            System.out.println("---");
            System.out.println(results.get()[0] + "\nGreen Probability: " + results.get()[1] + "%\nRed Probability: " + results.get()[2] + "%" + "\nAvg change in " + lookForwardAmount + " Candles: $" + results.get()[3] + "; +: " + results.get()[4] + "%, -: " + results.get()[5] + "%");
            System.out.println("Run Time: " + results.get()[6] + "s");
        }
        return results;
    }

    // -------------------------------TEMPORARY ELEMENTS END-----------------------------------------------


    public Stock getStock() {
        return stock;
    }

}
