package com.ejo.tradecompanion.elements.indicator;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.glowui.util.render.QuickDraw;
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
        //TODO: Make a bar on the bottom to render the macd histogram. Use the method i prepared below, but change the maY

        int focusY = (int)scene.getSize().getY() - 100;
        int focusPrice = 0;
        int maScale = 120;
        int histogramScale = 300;
        int maWidth = 1;

        QuickDraw.drawRect(new Vector(0, focusY - 100),new Vector(scene.getSize().getX() , focusY - 100),new ColorE(50,50,50,150));
        new LineUI(new Vector(0,focusY),new Vector(scene.getSize().getX(),focusY),ColorE.WHITE, LineUI.Type.DOTTED,.5).draw();

        for (CandleUI candle : candleList) {
            float[] macdData = getMACD().getData(candle.getOpenTime());
            float[] prevMacdData = getMACD().getData(candle.getOpenTime().getAdded(-candle.getStock().getTimeFrame().getSeconds()));
            float prevHeight = -(prevMacdData[0] - prevMacdData[1]);
            float height = -(macdData[0] - macdData[1]);
            ColorE color = ColorE.WHITE;
            if (height > 0) {
                color = ColorE.RED;
                if (prevHeight > height) color = ColorE.RED.alpha(150);
            } else if (height < 0) {
                color = ColorE.GREEN;
                if (prevHeight < height) color = ColorE.GREEN.alpha(150);
            }
            if (macdData[0] != -1) QuickDraw.drawRect(new Vector(candle.getPos().getX(), focusY), new Vector(candle.getBodySize().getX(),height * histogramScale),color);
        }


        drawMA(macd,0,candleList,focusY,focusPrice,maWidth,maScale);
        drawMA(macd,1,candleList,focusY,focusPrice,maWidth,maScale);
    }

    @Override
    protected void tickElement(Scene scene, Vector mousePos) {

    }

    @Override
    public boolean updateMouseOver(Vector mousePos) {
        return false;
    }


    private static void drawMA(IndicatorMACD macd, int lineIndex, ArrayList<CandleUI> candleList, int focusY, int focusPrice, int width, int scale) {
        ArrayList<Vector> points = new ArrayList<>();
        for (CandleUI candle : candleList) {
            float[] macdData = macd.getData(candle.getOpenTime());
            double maY = focusY - macdData[lineIndex] * scale + focusPrice * scale;
            if (macdData[lineIndex] != -1) points.add(new Vector(candle.getPos().getX() + (candle.getBodySize().getX() / 2), maY));
        }
        try {
            new LineUI(lineIndex == 0 ? ColorE.BLUE : ColorE.YELLOW, LineUI.Type.PLAIN, width, points.toArray(new Vector[0])).draw();
        } catch (Exception ignored) {
        }
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
