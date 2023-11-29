package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.SideBarUI;
import com.ejo.glowui.scene.elements.widget.ToggleUI;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.DownloadDrawUtil;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.tradecompanion.indicator.Indicator;
import com.ejo.tradecompanion.indicator.IndicatorEMA;
import com.ejo.tradecompanion.util.RenderUtil;

public class ChartViewScene extends Scene {

    private final Stock stock; //If the stock needs to be changed, switch to a new scene

    private final IndicatorEMA ema;

    //TODO: Add toggle to update EMA/SMA live AND add a full calculation menu with date ranges
    private final ToggleUI toggleEMA = new ToggleUI("EMA",new Vector(30,30),new Vector(100,25),ColorE.BLUE,new Container<>(false));
    private final ToggleUI toggleSMA = new ToggleUI("SMA",new Vector(30,30 + 30 + 5),new Vector(100,25),ColorE.BLUE,new Container<>(false));



    ProgressBarUI<Double> progressBarClosePercent = new ProgressBarUI<Double>(Vector.NULL,new Vector(100,30),ColorE.BLUE,new Container<>(0d),0,1);

    private final SideBarUI sidebar = new SideBarUI(SideBarUI.Type.TOP,120,true,ColorE.BLUE.alpha(120)
            ,toggleEMA
            ,toggleSMA
    );


    int candleOffset = 0;
    int candleYOffset = 0;
    double candleScaleX = 1;
    double candleScaleY = 200;




    public ChartViewScene(Stock stock) {
        super("Chart Viewer");
        this.stock = stock;
        this.ema = new IndicatorEMA(stock,50);
        ema.loadHistoricalData();
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

    @Override
    public void draw() {
        progressBarClosePercent.setPos(new Vector(0,getSize().getY() - 30));
        drawBackground(new ColorE(50,50,50));
        double candleWidth = 30;
        double candleSpace = 4;
        double focusY = getSize().getY() / 2;
        double focusPrice = getStock().getPrice();
        Vector candleScale = new Vector(candleScaleX, candleScaleY);
        DateTime time = new DateTime(2023,11,22,11,30);
        //DateTime time = getStock().getOpenTime();
        Indicator[] indicators = {};
        RenderUtil.drawStockData(this,getStock(),time,360,focusPrice,focusY,candleSpace,candleWidth,candleScale,indicators);
        super.draw();
    }

    @Override
    public void tick() {
        super.tick();
        getStock().updateLiveData();
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
    }

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
        candleScaleX += (double) scroll / 100;
        if (candleScaleX < 0.1) candleScaleX = .1;
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
    }

    public Stock getStock() {
        return stock;
    }

}
