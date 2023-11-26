package com.ejo.tradecompanion.util;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.render.CandleUI;
import com.ejo.stockdownloader.util.DrawUtil;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.tradecompanion.indicator.Indicator;
import com.ejo.tradecompanion.indicator.IndicatorEMA;
import com.ejo.tradecompanion.indicator.IndicatorSMA;

import java.util.ArrayList;

public class RenderUtil {

    public static void drawStockData(Scene scene, Stock stock, DateTime endTime, int candleCount, double focusPrice, double focusY, double separation, double candleWidth, Vector candleScale, Indicator... indicators) {
        //Define Candle List
        ArrayList<CandleUI> listCandle = new ArrayList<>();

        ArrayList<ArrayList<Vector>> listMA = new ArrayList<>();
        for (Indicator indicator : indicators) {
            if (indicator instanceof IndicatorEMA || indicator instanceof IndicatorSMA) listMA.add(new ArrayList<>());
        }

        DateTime openTime = endTime.equals(StockUtil.getAdjustedCurrentTime()) ? stock.getOpenTime() : endTime;
        try {
            for (int i = 0; i < candleCount; i++) {
                double x = scene.getSize().getX() - ((separation + candleWidth) * (i + 1)) * candleScale.getX();
                DateTime candleTime = new DateTime(openTime.getYear(), openTime.getMonth(), openTime.getDay(), openTime.getHour(), openTime.getMinute(), openTime.getSecond() - stock.getTimeFrame().getSeconds() * i);
                CandleUI historicalCandle = new CandleUI(stock, candleTime, x, focusY, focusPrice, candleWidth * candleScale.getX(), new Vector(1,candleScale.getY()));
                listCandle.add(historicalCandle);

                //For all MA indicators, add a point for each candle
                for (int j = 0; j < indicators.length; j++) {
                    Indicator indicator = indicators[j];
                    if (indicator instanceof IndicatorEMA || indicator instanceof IndicatorSMA) {
                        double maY = focusY -(indicator.getCloseValue(candleTime) * candleScale.getY()) + focusPrice*candleScale.getY();
                        if (indicator.getCloseValue(candleTime) != -1)
                            listMA.get(j).add(new Vector(x + candleWidth / 2,maY));
                    }
                }
            }
        } catch (NullPointerException ignored) {
        }

        //Draw Candles
        for (CandleUI candle : listCandle) {
            if (candle.getStock().getOpen(candle.getOpenTime()) != -1) candle.draw();
        }

        //Draw MA Lines
        for (int i = 0; i < indicators.length; i++) {
            Indicator ma = indicators[i];
            ArrayList<Vector> points = listMA.get(i);
            ColorE color = ma instanceof IndicatorEMA ? ColorE.YELLOW : ColorE.BLUE;
            try {
                new LineUI(color, LineUI.Type.PLAIN, 4d, points.toArray(new Vector[0])).draw();
            } catch (Exception ignored) {
            }
        }

        //Draw Tooltips - Done last as to make sure they are not covered by other candles or indicators
        for (CandleUI candle : listCandle) {
            if (candle.getStock().getOpen(candle.getOpenTime()) != -1) {
                candle.tick(scene); //Update Mouse Over
                if (candle.isMouseOver()) DrawUtil.drawCandleTooltip(candle, scene.getWindow().getScaledMousePos());
            }
        }

    }
}
