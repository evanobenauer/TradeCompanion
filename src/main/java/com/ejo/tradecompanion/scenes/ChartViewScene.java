package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.SideBarUI;
import com.ejo.glowui.scene.elements.widget.ToggleUI;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.DrawUtil;

public class ChartViewScene extends Scene {

    private final Stock stock; //If the stock needs to be changed, switch to a new scene

    //TODO: Add toggle to update EMA/SMA live AND add a full calculation menu with date ranges
    private final ToggleUI toggleEMA = new ToggleUI("EMA",new Vector(30,30),new Vector(100,25),ColorE.BLUE,new Container<>(false));
    private final ToggleUI toggleSMA = new ToggleUI("SMA",new Vector(30,30 + 30 + 5),new Vector(100,25),ColorE.BLUE,new Container<>(false));

    private final SideBarUI sidebar = new SideBarUI(SideBarUI.Type.TOP,120,true,ColorE.BLUE.alpha(120)
            ,toggleEMA
            ,toggleSMA
    );

    public ChartViewScene(Stock stock) {
        super("Chart Viewer");
        this.stock = stock;
        addElements(sidebar);
    }

    @Override
    public void draw() {
        drawBackground(new ColorE(50,50,50));
        super.draw();
        DateTime time = new DateTime(2023,11,22,11,30);
        DrawUtil.drawCandles(this,getStock(),time,getStock().getClose(time),getSize().getY() / 2,4,30,new Vector(1,300));
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
    }

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
    }

    public Stock getStock() {
        return stock;
    }

}
