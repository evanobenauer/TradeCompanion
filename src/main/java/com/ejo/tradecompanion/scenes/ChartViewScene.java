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
import com.ejo.glowui.util.Mouse;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.tradecompanion.indicator.Indicator;
import com.ejo.tradecompanion.indicator.IndicatorEMA;
import com.ejo.tradecompanion.indicator.IndicatorSMA;
import com.ejo.tradecompanion.util.RenderUtil;

import java.util.ArrayList;

public class ChartViewScene extends Scene {

    private final Stock stock; //If the stock needs to be changed, switch to a new scene

    private final IndicatorEMA ema;
    private final IndicatorSMA sma;

    //TODO: Add toggle to update EMA/SMA live AND add a full calculation menu with date ranges
    private final ToggleUI toggleEMA = new ToggleUI("EMA", new Vector(30, 30), new Vector(100, 25), ColorE.BLUE, new Container<>(false));
    private final ToggleUI toggleSMA = new ToggleUI("SMA", new Vector(30, 30 + 30 + 5), new Vector(100, 25), ColorE.BLUE, new Container<>(false));


    ProgressBarUI<Double> progressBarClosePercent = new ProgressBarUI<Double>(Vector.NULL, new Vector(100, 30), ColorE.BLUE, new Container<>(0d), 0, 1);

    private final SideBarUI sidebar = new SideBarUI(SideBarUI.Type.TOP, 120, true, ColorE.BLUE.alpha(120)
            , toggleEMA
            , toggleSMA
    );

    private int candleDateOffset = 0;
    private double candlePriceOffset = 0;
    private final VectorMod candleScale = new VectorMod(1,200);

    private boolean dragging = false;
    private Vector dragPosStart = Vector.NULL;
    private VectorMod dragPosTemp = Vector.NULL.getMod();
    private final VectorMod dragPosMain = Vector.NULL.getMod();


    public ChartViewScene(Stock stock) {
        super("Chart Viewer");
        this.stock = stock;

        this.ema = new IndicatorEMA(stock, 50);
        ema.loadHistoricalData(); //TODO: make sure to not include in constructor in future

        this.sma = new IndicatorSMA(stock, 50);
        sma.loadHistoricalData();

        progressBarClosePercent.setContainer(stock.getClosePercent());

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

        addElements(sidebar, progressBarClosePercent);
    }

    DateTime startTime = new DateTime(2023, 11, 30, 11, 30);

    @Override
    public void draw() {
        progressBarClosePercent.setPos(new Vector(0, getSize().getY() - 30));
        drawBackground(new ColorE(30, 30, 30));


        DateTime time = startTime.getAdded(candleDateOffset * stock.getTimeFrame().getSeconds());

        //TODO: Figure out how to jump over the time gap during closed hours
        while (!StockUtil.isPriceActive(getStock().isExtendedHours(),time)) {
            time = time.getAdded(-8 * 60 * 60);
        }


        double candleWidth = 30;
        double candleSpace = 4;
        double focusY = getSize().getY() / 2;
        double focusPrice = candlePriceOffset + 453;

        //TODO: Draw indicator if the candle price is below or above the focusPrice and off screen

        ArrayList<Indicator> indicatorsList = new ArrayList<>();
        if (toggleEMA.getContainer().get()) indicatorsList.add(ema);
        if (toggleSMA.getContainer().get()) indicatorsList.add(sma);

        Indicator[] indicators = indicatorsList.toArray(new Indicator[0]);
        RenderUtil.drawStockData(this, getStock(), time, 360, focusPrice, focusY, candleSpace, candleWidth, candleScale, indicators);
        super.draw();
    }

    @Override
    public void tick() {
        super.tick();
        getStock().updateLiveData();

        int separation = 4;
        int candleWidth = 30;

        if (dragging) {
            dragPosTemp = getWindow().getScaledMousePos().getSubtracted(dragPosStart).getMod();
            dragPosTemp.scale(1 / (candleScale.getX() * (separation + candleWidth)), 1 / candleScale.getY());

            candleDateOffset = (int) -(dragPosTemp.getX() + dragPosMain.getX());
            candlePriceOffset = dragPosTemp.getY() + dragPosMain.getY();
        }

    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
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

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
        float speed = (float) scroll / 100;
        candleScale.add(speed,0);
        if (candleScale.getX() < .1) candleScale.setX(.1);
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
    }

    public Stock getStock() {
        return stock;
    }

}
