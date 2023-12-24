package com.ejo.tradecompanion.util;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.elements.CandleUI;

import java.util.ArrayList;

public class ChartUtil {

    public static final ColorE WIDGET_COLOR = new ColorE(0, 160, 160);

    public static boolean isHoveredHorizontally(CandleUI candle, double candleSeparation, Vector mousePos) {
        return mousePos.getX() >= candle.getPos().getX() - candleSeparation * candle.getScale().getX() / 2 && mousePos.getX() <= candle.getPos().getX() + candle.getBodySize().getX() + candleSeparation * candle.getScale().getX() / 2;
    }

    //TODO: Make this only update when needed. Currently it constantly updates and causes lag
    public static ArrayList<CandleUI> getOnScreenCandles(Scene scene, Stock stock, DateTime lastTime, int candleCount, double focusPrice, double focusY, double separation, double candleWidth, Vector candleScale) {
        ArrayList<CandleUI> listCandle = new ArrayList<>();

        int currentCandles = 0;
        int loopCount = 0;
        while (currentCandles < candleCount) { //This is mildly inefficient. Maybe rewrite it someday ¯\_(ツ)_/¯
            DateTime candleTime = lastTime.getAdded(-loopCount * stock.getTimeFrame().getSeconds());

            if (!StockUtil.isPriceActive(stock.isExtendedHours(), candleTime)) { //Having extended hours turned off causes a lot of lag when rendering the same amount of candles
                loopCount++;
                continue;
            }

            double x = scene.getSize().getX() - ((separation + candleWidth) * (currentCandles + 1)) * candleScale.getX();
            if (!(x + candleWidth < 0 || x > scene.getSize().getX())) {
                CandleUI candle = new CandleUI(stock, candleTime, x, focusY, focusPrice, candleWidth, candleScale);
                listCandle.add(candle);
            }

            currentCandles++;
            loopCount++;
        }

        return listCandle;
    }

}
