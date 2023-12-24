package com.ejo.tradecompanion.elements.indicator;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.tradecompanion.data.indicator.IndicatorMA;
import com.ejo.tradecompanion.elements.CandleUI;

import java.util.ArrayList;

public class RenderMA extends ElementUI {

    private final IndicatorMA ma;
    private final ArrayList<CandleUI> candleList;

    public RenderMA(IndicatorMA ma, ArrayList<CandleUI> candleList) {
        super(Vector.NULL, true, true);
        this.ma = ma;
        this.candleList = candleList;
    }

    @Override
    protected void drawElement(Scene scene, Vector mousePos) {
        ArrayList<Vector> points = new ArrayList<>();
        for (CandleUI candle : getCandleList()) {
            double maY = candle.getFocusY() - (getMA().getCloseValue(candle.getOpenTime()) * candle.getScale().getY()) + candle.getFocusPrice() * candle.getScale().getY();
            if (getMA().getCloseValue(candle.getOpenTime()) != -1)
                points.add(new Vector(candle.getPos().getX() + (candle.getBodySize().getX() / 2), maY));//This is a little buggy with precise positioning of points for some reason
        }
        try {
            new LineUI(getMA().getColor(), LineUI.Type.PLAIN, 4d, points.toArray(new Vector[0])).draw();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void tickElement(Scene scene, Vector mousePos) {

    }

    @Override
    public boolean updateMouseOver(Vector mousePos) {
        return false;
    }

    public IndicatorMA getMA() {
        return ma;
    }

    public ArrayList<CandleUI> getCandleList() {
        return candleList;
    }
}
