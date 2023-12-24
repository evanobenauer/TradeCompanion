package com.ejo.tradecompanion.elements.indicator;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.tradecompanion.data.indicator.IndicatorMA;
import com.ejo.tradecompanion.data.indicator.IndicatorMACD;
import com.ejo.tradecompanion.elements.CandleUI;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RenderMACD extends ElementUI {

    private final IndicatorMACD macd;
    private final ArrayList<CandleUI> candleList;

    public RenderMACD(IndicatorMACD macd, ArrayList<CandleUI> candleList) {
        super(Vector.NULL, true, true);
        this.macd = macd;
        this.candleList = candleList;
    }

    @Override
    protected void drawElement(Scene scene, Vector mousePos) {
        macd.getMA1().setColor(new ColorE(Color.BLUE));
        macd.getMA2().setColor(new ColorE(Color.YELLOW));

        new RenderMA(macd.getMA1(),candleList).draw(scene);
        new RenderMA(macd.getMA2(),candleList).draw(scene);
        //TODO: Make a bar on the bottom to render the macd histogram. Use the method i prepared below, but change the maY

        int focusY = 100;
        int focusPrice = 450;
        int scale = 20;
        int width = 1;
        //drawMA(macd.getMA1(),candleList,focusY,focusPrice,width,scale);
        //drawMA(macd.getMA2(),candleList,focusY,focusPrice,width,scale);
    }

    @Override
    protected void tickElement(Scene scene, Vector mousePos) {

    }

    @Override
    public boolean updateMouseOver(Vector mousePos) {
        return false;
    }


    private static void drawMA(IndicatorMA ma, ArrayList<CandleUI> candleList, int focusY, int focusPrice, int width, int scale) {
        ArrayList<Vector> points = new ArrayList<>();
        for (CandleUI candle : candleList) {
            double maY = focusY - ma.getCloseValue(candle.getOpenTime()) * scale + focusPrice * scale;
            if (ma.getCloseValue(candle.getOpenTime()) != -1) points.add(new Vector(candle.getPos().getX() + (candle.getBodySize().getX() / 2), maY));
        }
        try {
            new LineUI(ma.getColor(), LineUI.Type.PLAIN, width, points.toArray(new Vector[0])).draw();
        } catch (Exception ignored) {
        }
    }


    public IndicatorMACD getMACD() {
        return macd;
    }

    public ArrayList<CandleUI> getCandleList() {
        return candleList;
    }
}
