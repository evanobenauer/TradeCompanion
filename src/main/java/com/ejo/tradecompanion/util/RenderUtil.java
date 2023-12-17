package com.ejo.tradecompanion.util;

import com.ejo.glowlib.math.Angle;
import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowui.scene.elements.shape.CircleUI;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.data.indicator.Indicator;
import com.ejo.tradecompanion.data.indicator.IndicatorMA;
import com.ejo.tradecompanion.elements.CandleUI;

import java.awt.*;
import java.util.ArrayList;

public class RenderUtil {

    public static void drawCandlesFromData(ArrayList<CandleUI> listCandle, Indicator... indicators) {
        //Define MA Lists
        ArrayList<Indicator> maList = new ArrayList<>();
        ArrayList<ArrayList<Vector>> listPointsMAs = new ArrayList<>();
        for (Indicator indicator : indicators) {
            if (indicator instanceof IndicatorMA) {
                maList.add(indicator);
                listPointsMAs.add(new ArrayList<>());
            }
        }

        //Draw Candles
        for (CandleUI candle : listCandle) {
            candle.setGreen(new ColorE(0, 255, 255)).setRed(new ColorE(255, 100, 0)).setColorNull(new ColorE(255, 0, 255));
            if (candle.getStock().getOpen(candle.getOpenTime()) != -1) candle.draw();

            //For all MA indicators, add a point for each candle
            for (int j = 0; j < maList.size(); j++) {
                Indicator indicator = indicators[j];
                double maY = candle.getFocusY() - (indicator.getCloseValue(candle.getOpenTime()) * candle.getScale().getY()) + candle.getFocusPrice() * candle.getScale().getY();
                if (indicator.getCloseValue(candle.getOpenTime()) != -1) {

                    //This is a little buggy with precise positioning of points for some reason
                    listPointsMAs.get(j).add(new Vector(candle.getPos().getX() + (candle.getBodySize().getX() / 2), maY)); //This one is more akin to what is realistic
                }
            }
        }

        //Draw MA Lines
        for (int i = 0; i < indicators.length; i++) {
            Indicator indicator = indicators[i];
            if (indicator instanceof IndicatorMA ma) {
                ArrayList<Vector> points = listPointsMAs.get(i);
                try {
                    new LineUI(ma.getColor(), LineUI.Type.PLAIN, 4d, points.toArray(new Vector[0])).draw();
                } catch (Exception ignored) {
                }
            }
        }

    }

    //Size default is 20
    public static void drawProgressWheel(double min, double current, double max, int size, Vector mousePos) {
        double progress = (current - min) / (max - min);
        //ColorE color = new ColorE((int) (255 * (1 - progress)), (int) (255 * (progress)), 0, 255);
        ColorE color = ChartUtil.WIDGET_COLOR;
        Vector pos = mousePos.getAdded(new Vector(size + (double) size /4, (double) -size /4));
        new CircleUI(pos, ColorE.BLACK, (double) size + (double) size /4, CircleUI.Type.MEDIUM).draw();
        new CircleUI(pos, color, size, new Angle(360 * (progress), true), CircleUI.Type.MEDIUM).draw();
        new CircleUI(pos, ColorE.BLACK, (double) size - (double) size /2, CircleUI.Type.MEDIUM).draw();
    }

    public static void drawCandleTooltip(CandleUI candle, Vector mousePos) {
        Stock stock = candle.getStock();
        int textSize = 10;
        double x = mousePos.getX() - 96;
        double y = mousePos.getY() - textSize * 5 - 7;

        //Bound X Left
        if (x < 0) {
            x = 0;
            mousePos = new Vector(96, mousePos.getY());
        }

        //Bound Y Up
        if (y < 0) {
            y = 0;
            mousePos = new Vector(mousePos.getX(), textSize * 5 + 7);
        }

        //Round Data
        double open = MathE.roundDouble(stock.getOpen(candle.getOpenTime()), 2);
        double close = MathE.roundDouble(stock.getClose(candle.getOpenTime()), 2);
        double min = MathE.roundDouble(stock.getMin(candle.getOpenTime()), 2);
        double max = MathE.roundDouble(stock.getMax(candle.getOpenTime()), 2);

        //Draw Background
        QuickDraw.drawRect(new Vector(x - 2, y), new Vector(mousePos.getX() - x + 2, mousePos.getY() - y - 1), new ColorE(0, 125, 200, 200));

        //Draw Data
        QuickDraw.drawText(candle.getOpenTime().toString(), new Font("Arial", Font.PLAIN, textSize), new Vector(x, y), ColorE.WHITE);
        QuickDraw.drawText("Open:" + open, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize), ColorE.WHITE);
        QuickDraw.drawText("Close:" + close, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 2), ColorE.WHITE);
        QuickDraw.drawText("Min:" + min, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 3), ColorE.WHITE);
        QuickDraw.drawText("Max:" + max, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 4), ColorE.WHITE);

    }
}
