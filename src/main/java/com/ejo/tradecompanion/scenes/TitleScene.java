package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.setting.Setting;
import com.ejo.glowlib.setting.SettingManager;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.scene.elements.shape.GradientRectangleUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.ModeCycleUI;
import com.ejo.glowui.scene.elements.widget.TextFieldUI;
import com.ejo.glowui.scene.elements.widget.ToggleUI;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.util.TimeFrame;
import com.ejo.tradecompanion.web.StockScraper;

import java.awt.*;

public class TitleScene extends Scene {

    private static final ColorE WIDGET_COLOR = new ColorE(0, 110, 110);

    private Stock stock;

    private final Setting<String> stockTicker = new Setting<>("stockTicker", "");
    private final Setting<TimeFrame> timeFrame = new Setting<>("timeFrame", TimeFrame.ONE_MINUTE);
    private final Setting<Boolean> extendedHours = new Setting<>("extendedHours", false);

    private final TextUI textTitle = new TextUI("Trade Companion", new Font("Arial Black", Font.BOLD, 50), Vector.NULL, ColorE.WHITE);
    private final TextFieldUI fieldStockTicker = new TextFieldUI(Vector.NULL, new Vector(100, 20), ColorE.WHITE, stockTicker, "Stock", false);
    private final ModeCycleUI<TimeFrame> modeTimeFrame = new ModeCycleUI<>(Vector.NULL, new Vector(100, 20), WIDGET_COLOR, timeFrame, TimeFrame.ONE_SECOND, TimeFrame.FIVE_SECONDS, TimeFrame.THIRTY_SECONDS, TimeFrame.ONE_MINUTE, TimeFrame.FIVE_MINUTES, TimeFrame.THIRTY_MINUTES, TimeFrame.ONE_HOUR, TimeFrame.TWO_HOUR, TimeFrame.FOUR_HOUR, TimeFrame.ONE_DAY);
    private final ToggleUI toggleExtendedHours = new ToggleUI("Extended Hours",Vector.NULL,new Vector(100,20), WIDGET_COLOR,extendedHours);
    private final ProgressBarUI<Double> progressBarLoadStock = new ProgressBarUI<>(Vector.NULL,new Vector(300,30), WIDGET_COLOR,new Container<>(0d),0,1);

    private final ButtonUI buttonOpenChart = new ButtonUI("Open Chart", Vector.NULL, new Vector(200, 60), new ColorE(0, 110, 110), ButtonUI.MouseButton.LEFT, () -> {
        if (stockTicker.get().equals("") || (this.stock != null && this.stock.isProgressActive())) return;

        //TODO: add checker to see if a file exists. also include a warning text

        Thread thread = new Thread(() -> {
            SettingManager.getDefaultManager().saveAll();
            this.stock = new Stock(stockTicker.get(),timeFrame.get(),extendedHours.get(), StockScraper.PriceSource.MARKETWATCH,false);
            progressBarLoadStock.setContainer(stock.getProgressContainer());
            stock.loadHistoricalData();
            getWindow().setScene(new ChartViewScene(stock));
        });
        thread.setDaemon(true);
        thread.start();
    });


    public TitleScene() {
        super("Title Scene");
        addElements(textTitle, fieldStockTicker, modeTimeFrame, buttonOpenChart,toggleExtendedHours, progressBarLoadStock);
        SettingManager.getDefaultManager().loadAll();
    }


    @Override
    public void draw() {
        updateElementPositions();
        new GradientRectangleUI(Vector.NULL,getSize(),new ColorE(0, 255, 255).alpha(20),new ColorE(0, 0, 0), GradientRectangleUI.Type.VERTICAL).draw();
        super.draw();
    }


    @Override
    public void tick() {
        super.tick();
        progressBarLoadStock.setEnabled(stock != null && stock.isProgressActive());
    }

    private float sinStep = 0;

    private void updateElementPositions() {
        double yOffset = -40;

        //Set Floating Title
        textTitle.setPos(getSize().getMultiplied(.5d).getAdded(textTitle.getSize().getMultiplied(-.5)).getAdded(0, yOffset));
        textTitle.setPos(textTitle.getPos().getAdded(new Vector(0, Math.sin(sinStep) * 8)));
        sinStep += 0.05;
        if (sinStep >= Math.PI * 2) sinStep = 0;

        //Set Widget Positions
        fieldStockTicker.setPos(getSize().getMultiplied(.5d).getAdded(fieldStockTicker.getSize().getMultiplied(-.5)).getAdded(-fieldStockTicker.getSize().getX(), 140).getAdded(0, yOffset));
        modeTimeFrame.setPos(getSize().getMultiplied(.5d).getAdded(modeTimeFrame.getSize().getMultiplied(-.5)).getAdded(+modeTimeFrame.getSize().getX(), 140).getAdded(0, yOffset));
        toggleExtendedHours.setPos(getSize().getMultiplied(.5d).getAdded(toggleExtendedHours.getSize().getMultiplied(-.5)).getAdded(0,140 + 30).getAdded(0,yOffset));

        buttonOpenChart.setPos(getSize().getMultiplied(.5d).getAdded(buttonOpenChart.getSize().getMultiplied(-.5)).getAdded(0, textTitle.getFont().getSize() + 30).getAdded(0, yOffset));

        progressBarLoadStock.setPos(getSize().getMultiplied(.5d).getAdded(progressBarLoadStock.getSize().getMultiplied(-.5)).getAdded(0, 220).getAdded(0, yOffset));

    }

}
