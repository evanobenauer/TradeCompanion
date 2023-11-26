package com.ejo.tradecompanion.scenes;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.util.Key;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.render.CandleUI;
import com.ejo.stockdownloader.util.DrawUtil;
import com.ejo.stockdownloader.util.TimeFrame;

import java.util.ArrayList;
import java.util.Map;

public class ProbabilityScene extends Scene {

    Stock stock = new Stock("SPY", TimeFrame.ONE_MINUTE,true, Stock.PriceSource.MARKETWATCH);
    TextUI text = new TextUI("", Fonts.getDefaultFont(30),Vector.NULL,ColorE.WHITE);

    public ProbabilityScene() {
        super("Probability Scene");
        addElements(text);
    }

    ArrayList<Long> dateTimeIDList = new ArrayList<>();

    DateTime time = new DateTime(2023,9,21,11,04,00);
    double x = 3;

    @Override
    public void draw() {
        drawBackground(new ColorE(50, 50, 50, 255));
        super.draw();

        CandleUI candle = new CandleUI(stock,time,3,100,stock.getOpen(time),30,new Vector(1,200));
        candle.tick(this);
        candle.draw(this);
        if (candle.isMouseOver()) DrawUtil.drawCandleTooltip(candle,getWindow().getScaledMousePos());
        QuickDraw.drawText(candle.getOpenTime().toString(),Fonts.getDefaultFont(20),candle.getPos().getAdded(candle.getWidth(),0),ColorE.WHITE);

        double x = 3 + this.x;
        for (Long id : dateTimeIDList) {
            if (x > getSize().getX()) continue;
            DateTime time = new DateTime(id);
            CandleUI candleUI = new CandleUI(stock, time, x, 300, stock.getOpen(time), 30, new Vector(1, 200));
            if (x + 30 > 0) {
                candleUI.draw(this);
                candleUI.tick(this);
                if (candleUI.isMouseOver()) DrawUtil.drawCandleTooltip(candleUI, getWindow().getScaledMousePos());
            }
            x += candleUI.getWidth() + 3;
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    int timeAdd = 0;

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (action != Key.ACTION_RELEASE) {
            if (key == Key.KEY_RIGHT.getId()) x -= 20;
            if (key == Key.KEY_LEFT.getId()) x += 20;
            if (key == Key.KEY_J.getId()) x = 0;
        }

        if (action != Key.ACTION_PRESS) return;
        if (key == Key.KEY_H.getId()) {
            time = new DateTime(2021,7,14,14,13,timeAdd);
            timeAdd += 60;
        }

        if (key == Key.KEY_C.getId()) {
            Thread thread = new Thread(() -> {
                runProbabilityCalculation(stock,time,.02f,true);
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void runProbabilityCalculation(Stock stock, DateTime candleTime, float marginPrice, boolean doPriceScaling) {
        dateTimeIDList.clear();

        float open = stock.getOpen(candleTime);
        float close = stock.getClose(candleTime);
        float min = stock.getMin(candleTime);
        float max = stock.getMax(candleTime);
        float topWick = getTopWickSize(max,open,close);
        float bottomWick = getBottomWickSize(min,open,close);

        int nextGreen = 0;
        int nextRed = 0;

        //Get similar candles from all data
        ArrayList<Long> similarCandlesList = new ArrayList<>();
        int similarCandleCount = 0;
        for (Map.Entry<Long, String[]> data : stock.getHistoricalData().entrySet()) { //Loops through all stock data
            float dataOpen = Float.parseFloat(data.getValue()[0]);
            float dataClose = Float.parseFloat(data.getValue()[1]);
            float dataMin = Float.parseFloat(data.getValue()[2]);
            float dataMax = Float.parseFloat(data.getValue()[3]);

            //This value scales based on the total cap of the stock over history so similarities have a chance
            float pricingScale = doPriceScaling ? dataOpen / open : 1;

            boolean bodySize = isWithinMargin(open,dataOpen / pricingScale,marginPrice) && isWithinMargin(close,dataClose / pricingScale,marginPrice);//isWithinMargin(ocDiff,getOpenCloseDifference(dataOpen,dataClose) / pricingScale,marginPrice);
            boolean topWickSize = isWithinMargin(topWick, getTopWickSize(dataMax,dataOpen,dataClose) / pricingScale,marginPrice);
            boolean bottomWickSize = isWithinMargin(bottomWick, getBottomWickSize(dataMin,dataOpen,dataClose) / pricingScale,marginPrice);

            if (bodySize && topWickSize && bottomWickSize) {
                similarCandlesList.add(data.getKey());
                long id = data.getKey();
                DateTime thisTime = new DateTime(id);
                if (stock.getClose(thisTime.getAdded(60)) > stock.getOpen(thisTime.getAdded(60))) {
                    nextGreen++;
                }
                if (stock.getClose(thisTime.getAdded(60)) < stock.getOpen(thisTime.getAdded(60))) {
                    nextRed++;
                }
                similarCandleCount++;
            }
        }

        dateTimeIDList.addAll(similarCandlesList);

        /*
        int nextGreen = 0;
        int nextRed = 0;

        //Sort out candles that the previous is unsimilar
        ArrayList<Long> secondSimilarCandlesList = new ArrayList<>();
        for (long id : similarCandlesList) {
            if (isPrevCandleSimilar(stock, candleTime, new DateTime(id), 1, doPriceScaling, marginPrice)) {
                secondSimilarCandlesList.add(id);
            }
        }

        //Sort out candles that the previous-previous is unsimilar
        for (long id : secondSimilarCandlesList) {
            if (isPrevCandleSimilar(stock, candleTime, new DateTime(id), 2, doPriceScaling, marginPrice)) {
                dateTimeIDList.add(id);
                DateTime thisTime = new DateTime(id);
                if (stock.getClose(thisTime.getAdded(60)) > stock.getOpen(thisTime.getAdded(60))) {
                    nextGreen++;
                }
                if (stock.getClose(thisTime.getAdded(60)) < stock.getOpen(thisTime.getAdded(60))) {
                    nextRed++;
                }
                similarCandleCount++;
            }
        }
        */


        text.setText(similarCandleCount + "\\nGreen Probability: " + MathE.roundDouble((double) nextGreen /similarCandleCount * 100,1) + "%\\nRed Probability: " + MathE.roundDouble((double) nextRed /similarCandleCount * 100,1) + "%");
    }

    private boolean isPrevCandleSimilar(Stock stock, DateTime mainCandleTime, DateTime similarCandleTime, int candlesBack, boolean doPriceScaling, float marginPrice) {
        float prevOpen = stock.getOpen(mainCandleTime.getAdded(-stock.getTimeFrame().getSeconds() * candlesBack));
        float prevClose = stock.getClose(mainCandleTime.getAdded(-stock.getTimeFrame().getSeconds() * candlesBack));
        float prevMin = stock.getMin(mainCandleTime.getAdded(-stock.getTimeFrame().getSeconds() * candlesBack));
        float prevMax = stock.getMax(mainCandleTime.getAdded(-stock.getTimeFrame().getSeconds() * candlesBack));

        float prevTopWick = getTopWickSize(prevMax, prevOpen, prevClose);
        float prevBottomWick = getBottomWickSize(prevMin, prevOpen, prevClose);

        float prevDataOpen = stock.getOpen(similarCandleTime.getAdded(-stock.getTimeFrame().getSeconds() * candlesBack));
        float prevDataClose = stock.getClose(similarCandleTime.getAdded(-stock.getTimeFrame().getSeconds() * candlesBack));
        float prevDataMin = stock.getMin(similarCandleTime.getAdded(-stock.getTimeFrame().getSeconds() * candlesBack));
        float prevDataMax = stock.getMax(similarCandleTime.getAdded(-stock.getTimeFrame().getSeconds() * candlesBack));

        float pricingScale = doPriceScaling ? prevDataOpen / prevOpen : 1;

        boolean bodySize = isWithinMargin(prevOpen, prevDataOpen / pricingScale, marginPrice) && isWithinMargin(prevClose, prevDataClose / pricingScale, marginPrice);//isWithinMargin(ocDiff,getOpenCloseDifference(dataOpen,dataClose) / pricingScale,marginPrice);
        boolean topWickSize = isWithinMargin(prevTopWick, getTopWickSize(prevDataMax, prevDataOpen, prevDataClose) / pricingScale, marginPrice);
        boolean bottomWickSize = isWithinMargin(prevBottomWick, getBottomWickSize(prevDataMin, prevDataOpen, prevDataClose) / pricingScale, marginPrice);

        return (bodySize && topWickSize && bottomWickSize);
    }

    private float getOpenCloseDifference(float open, float close) {
        return close - open;
    }

    private float getTopWickSize(float max, float open, float close) {
        return getOpenCloseDifference(open,close) >= 0 ? max - close : max - open;
    }

    private float getBottomWickSize(float min, float open, float close) {
        return getOpenCloseDifference(open,close) <= 0 ? close - min : open - min;
    }

    private boolean isWithinMargin(float mainValue, float testValue, float margin) {
        return (mainValue + margin >= testValue && mainValue - margin <= testValue);
    }

    @Override
    public void onMouseScroll(int scroll, Vector mousePos) {
        super.onMouseScroll(scroll, mousePos);
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
    }
}
