package com.ejo.tradecompanion.scenes.experimental;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.util.Key;
import com.ejo.glowui.util.Mouse;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.render.CandleUI;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.util.TimeFrame;
import com.ejo.tradecompanion.util.RenderUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This is an experimental scene for getting the probability function indicator to work
 */
public class ProbabilityScene extends Scene {

    Stock stock = new Stock("SPY", TimeFrame.ONE_MINUTE,true, Stock.PriceSource.MARKETWATCH);
    DateTime time = new DateTime(2023,9,21,11,4,0);

    TextUI text = new TextUI("", Fonts.getDefaultFont(30),Vector.NULL,ColorE.WHITE);

    public ProbabilityScene() {
        super("Probability Scene");
        addElements(text);
    }

    ArrayList<Long> similarCandles = new ArrayList<>();

    @Override
    public void draw() {
        //if (stock.getOpenTime() != null) time = stock.getOpenTime().getAdded(-stock.getTimeFrame().getSeconds());

        drawBackground(new ColorE(50, 50, 50, 255));
        try {
            super.draw();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        //Draw Test Candle
        CandleUI candle = new CandleUI(stock,time,3,getSize().getY() / 2 + 100,stock.getOpen(time),30,new Vector(1,200));
        candle.tick(this);
        candle.draw(this);
        if (candle.isMouseOver()) {
            RenderUtil.drawCandleTooltip(candle,getWindow().getScaledMousePos());
            ArrayList<CandleUI> candList = new ArrayList<>();
            double pX = getSize().getX() / 2;
            double pY = getSize().getY() - 200;
            double focPrice  = stock.getOpen(time);
            //pre-candles
            for (int i = 0; i < 5; i++) {
                candList.add(new CandleUI(stock, time.getAdded(-stock.getTimeFrame().getSeconds()*i), pX - i*31, pY, focPrice, 30, new Vector(1, 400)));
            }
            //post-candles
            for (int i = 1; i < 4; i++) {
                candList.add(new CandleUI(stock, time.getAdded(stock.getTimeFrame().getSeconds()*i), pX + i*31, pY, focPrice, 30, new Vector(1, 400)));
            }

            QuickDraw.drawRect(candList.get(5).getPos().getSubtracted(new Vector(31,20)),new Vector(31,100),new ColorE(0,125,200,100));

            for (CandleUI candley: candList) {
                candley.draw(this);
                candley.tick(this);
            }
        }
        QuickDraw.drawText(candle.getOpenTime().toString(), Fonts.getDefaultFont(20),candle.getPos().getAdded(candle.getWidth(),0),ColorE.WHITE);

        //Draw Similar Candles
        double x = 3;
        for (Long id : similarCandles) {
            if (x > getSize().getX()) continue;
            DateTime time = new DateTime(id);
            CandleUI candleUI = new CandleUI(stock, time, x, getSize().getY() / 2, stock.getOpen(time), 30, new Vector(1, 500));
            if (x + 30 > 0) {
                candleUI.draw(this);
                candleUI.tick(this);
                if (candleUI.isMouseOver()) {
                    RenderUtil.drawCandleTooltip(candleUI, getWindow().getScaledMousePos());
                    ArrayList<CandleUI> candList = new ArrayList<>();
                    double pX = getSize().getX() / 2;
                    double pY = getSize().getY() - 200;
                    double focPrice  = stock.getOpen(time);
                    //pre-candles
                    for (int i = 0; i < 5; i++) {
                        candList.add(new CandleUI(stock, time.getAdded(-stock.getTimeFrame().getSeconds()*i), pX - i*31, pY, focPrice, 30, new Vector(1, 400)));
                    }
                    //post-candles
                    for (int i = 1; i < 4; i++) {
                        candList.add(new CandleUI(stock, time.getAdded(stock.getTimeFrame().getSeconds()*i), pX + i*31, pY, focPrice, 30, new Vector(1, 400)));
                    }

                    QuickDraw.drawRect(candList.get(5).getPos().getSubtracted(new Vector(31,20)),new Vector(31,100),new ColorE(0,125,200,100));

                    for (CandleUI candley: candList) {
                        candley.draw(this);
                        candley.tick(this);
                    }
                }
            }
            x += candleUI.getWidth() + 3;
        }

        QuickDraw.drawText(String.valueOf(wins),Fonts.getDefaultFont(20),new Vector(2,getSize().getY() - 44),ColorE.WHITE);
        QuickDraw.drawText(String.valueOf(losses),Fonts.getDefaultFont(20),new Vector(2,getSize().getY() - 22),ColorE.WHITE);
    }

    @Override
    public void tick() {
        super.tick();
        //stock.updateLiveData(.5,true);
    }

    int wins = 0;
    int losses = 0;

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(button, action, mods, mousePos);
        if (action != Mouse.ACTION_CLICK) return;
        if (button == Mouse.BUTTON_LEFT.getId()) wins++;
        if (button == Mouse.BUTTON_RIGHT.getId()) losses++;
        if (button == Mouse.BUTTON_MIDDLE.getId()) wins = losses = 0;
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if (action == Key.ACTION_RELEASE) return;

        int speed = 1;
        if (key == Key.KEY_RIGHT.getId()) time = time.getAdded(speed * stock.getTimeFrame().getSeconds());
        if (key == Key.KEY_LEFT.getId()) time = time.getAdded(-speed * stock.getTimeFrame().getSeconds());
        if (key == Key.KEY_DOWN.getId()) time = time.getAdded(-60*60*24);
        if (key == Key.KEY_UP.getId()) time = time.getAdded(60*60*24);

        if (key == Key.KEY_C.getId()) {
            Thread thread = new Thread(() -> {
                DateTime time = this.time;
                System.out.println("Starting calculation");
                // -------------------------------------
                float precision = .03f;
                float precisionLooseness = 0;
                float precisionDecrease = 0;//.005f;
                int lookbackAmount = 4;
                boolean priceScale = false;
                boolean ignoreWicks = true;
                boolean includeAfterHours = false;

                // ----------------------------------------------
                ArrayList<ArrayList<Long>> biggie = new ArrayList<>();
                biggie.add(getSimilarCandleIDs(stock, time, precision + precisionLooseness, priceScale,ignoreWicks,includeAfterHours));
                precisionLooseness += precisionDecrease;

                for (int l = 1; l <= lookbackAmount; l++) {
                    biggie.add(filterSimilarCandlesFromPrevious(stock, time, precision + precisionLooseness, priceScale, ignoreWicks,includeAfterHours,biggie.get(l - 1), l));
                    precisionLooseness += precisionDecrease;
                }

                this.similarCandles = biggie.get(lookbackAmount);
            });
            thread.setDaemon(true);
            thread.start();
        }

    }

    //TODO: Find a way to throw out garbage data from 2000-2003. It messes stuff up. Maybe just exclude it?
    private ArrayList<Long> getSimilarCandleIDs(Stock stock, DateTime candleTime, float marginPrice, boolean doPriceScaling, boolean ignoreWicks, boolean includeAfterHours) {
        ArrayList<Long> similarCandleList = new ArrayList<>();
        int similarCandleCount = 0; //Maybe do something with this lol
        int nextGreen = 0;
        int nextRed = 0;
        float avgCloseInThreeCandles = 0;

        long mainID = candleTime.getDateTimeID();
        float mainOpen = stock.getOpen(candleTime);
        float mainClose = stock.getClose(candleTime);
        float mainMin = stock.getMin(candleTime);
        float mainMax = stock.getMax(candleTime);

        for (Map.Entry<Long, String[]> testData : stock.getHistoricalData().entrySet()) { //Loops through all stock data
            if (testData.getKey() == mainID) continue;
            if (!includeAfterHours && !StockUtil.isTradingHours(new DateTime(testData.getKey()))) continue;
            float testOpen = Float.parseFloat(testData.getValue()[0]);
            float testClose = Float.parseFloat(testData.getValue()[1]);
            float testMin = Float.parseFloat(testData.getValue()[2]);
            float testMax = Float.parseFloat(testData.getValue()[3]);

            //This value scales based on the total cap of the stock over history so similarities have a chance
            float pricingScale = doPriceScaling ? testOpen / mainOpen : 1;
            if (areCandlesSimilar(new float[]{mainOpen,mainClose,mainMin,mainMax},new float[]{testOpen,testClose,testMin,testMax},marginPrice,pricingScale,ignoreWicks)) {
                similarCandleList.add(testData.getKey());
                DateTime thisTime = new DateTime(testData.getKey());
                int offset = stock.getTimeFrame().getSeconds();
                float[] data = stock.getData(thisTime.getAdded(offset));
                if (data[1] > data[0]) nextGreen++;
                if (data[1] < data[0]) nextRed++;
                avgCloseInThreeCandles += stock.getClose(thisTime.getAdded(offset * 3)) - stock.getClose(thisTime);
                similarCandleCount++;
            }
        }
        if (similarCandleCount == 0) avgCloseInThreeCandles = 0;
        else avgCloseInThreeCandles /= similarCandleCount;
        avgCloseInThreeCandles = (float) MathE.roundDouble(avgCloseInThreeCandles,2);
        System.out.println(similarCandleCount);
        return similarCandleList;
    }

    private ArrayList<Long> filterSimilarCandlesFromPrevious(Stock stock, DateTime candleTime, float marginPrice, boolean doPriceScaling, boolean ignoreWicks, boolean includeAfterHours, ArrayList<Long> similarCandles, int previous) {
        ArrayList<Long> similarCandleList = new ArrayList<>();
        int similarCandleCount = 0; //Maybe do something with this lol
        int nextGreen = 0;
        int nextRed = 0;
        int greenCloseInThreeCandles = 0;
        int redCloseInThreeCandles = 0;
        float avgCloseInThreeCandles = 0;

        HashMap<Long, String[]> historicalData = stock.getHistoricalData();

        DateTime prevCandleTime = candleTime.getAdded(-stock.getTimeFrame().getSeconds() * previous);
        float prevMainOpen = stock.getOpen(prevCandleTime);
        float prevMainClose = stock.getClose(prevCandleTime);
        float prevMainMin = stock.getMin(prevCandleTime);
        float prevMainMax = stock.getMax(prevCandleTime);

        //Checks if the previous candle is similar also. This will filter out all who do not have a similar previous candle
        for (Long id : similarCandles) {
            Long prevID = new DateTime(id).getAdded(-stock.getTimeFrame().getSeconds() * previous).getDateTimeID();
            if (!includeAfterHours && !StockUtil.isTradingHours(new DateTime(id))) continue;
            try {
                float prevTestOpen = Float.parseFloat(historicalData.get(prevID)[0]);
                float prevTestClose = Float.parseFloat(historicalData.get(prevID)[1]);
                float prevTestMain = Float.parseFloat(historicalData.get(prevID)[2]);
                float prevTestMax = Float.parseFloat(historicalData.get(prevID)[3]);


                float pricingScale = doPriceScaling ? prevTestOpen / prevMainOpen : 1;
                if (areCandlesSimilar(new float[]{prevMainOpen, prevMainClose, prevMainMin, prevMainMax}, new float[]{prevTestOpen, prevTestClose, prevTestMain, prevTestMax}, marginPrice, pricingScale,ignoreWicks)) {
                    similarCandleList.add(id);
                    DateTime thisTime = new DateTime(id);
                    int offset = stock.getTimeFrame().getSeconds();
                    float[] data = stock.getData(thisTime.getAdded(offset));
                    if (data[1] > data[0]) nextGreen++;
                    if (data[1] < data[0]) nextRed++;
                    float[] futureData = stock.getData(thisTime.getAdded(offset * 3));
                    float thisClose = stock.getClose(thisTime);
                    avgCloseInThreeCandles += futureData[1] - thisClose;
                    if (futureData[1] > thisClose) greenCloseInThreeCandles++;
                    if (futureData[1] < thisClose) redCloseInThreeCandles++;
                    //More can be added here. Like nextNextNextGreen to look farther in the future based on the built pattern
                    // You could also check what the live close price is a few candles ahead to see the average price chance over time
                    similarCandleCount++;
                }
            } catch (Exception e) {
                continue;
            }
        }
        if (similarCandleCount == 0) avgCloseInThreeCandles = 0;
        else avgCloseInThreeCandles /= similarCandleCount;
        avgCloseInThreeCandles = (float) MathE.roundDouble(avgCloseInThreeCandles,2);
        System.out.println(similarCandleCount);
        text.setText(similarCandleCount + "\\nGreen Probability: " + MathE.roundDouble((double) nextGreen /similarCandleCount * 100,1) + "%\\nRed Probability: " + MathE.roundDouble((double) nextRed /similarCandleCount * 100,1) + "%" + "\\nAvg change in 3 Candles: $" + avgCloseInThreeCandles + "; +: " + MathE.roundDouble((double) greenCloseInThreeCandles /similarCandleCount * 100,1) + "%, -: " + MathE.roundDouble((double) redCloseInThreeCandles /similarCandleCount * 100,1) + "%");
        return similarCandleList;
    }


    //TODO: Testing how candles are similar is the big challenge. Mess around with this more to get a good result
    private boolean areCandlesSimilar(float[] mainData, float[] testData, float marginPrice, float pricingScale, boolean ignoreWicks) {
        float mainOpen = mainData[0];
        float mainClose = mainData[1];
        float mainMin = mainData[2];
        float mainMax = mainData[3];
        float mainTopWick = getTopWickSize(mainMax,mainOpen,mainClose);
        float mainBottomWick = getBottomWickSize(mainMin,mainOpen,mainClose);

        float testOpen =  testData[0];
        float testClose = testData[1];
        float testMin = testData[2];
        float testMax = testData[3];

        //TODO: Maybe use a method that adds all the variation together into one total percent that has to be below a max percent
        //TODO: since the data may cause candles to start at the wrong y pos, maybe add some sort of check for this?
        //boolean bodySize = isWithinMargin(mainOpen,testOpen / pricingScale,marginPrice) && isWithinMargin(mainClose,testClose / pricingScale,marginPrice);//isWithinMargin(ocDiff,getOpenCloseDifference(dataOpen,dataClose) / pricingScale,marginPrice);
        boolean bodySize = isWithinMargin(getOpenCloseDifference(mainOpen,mainClose),getOpenCloseDifference(testOpen,testClose) / pricingScale,marginPrice);
        boolean topWickSize;
        boolean bottomWickSize;
        if (ignoreWicks) {
            topWickSize = true;
            bottomWickSize = true;
        } else {
            topWickSize = isWithinMargin(mainTopWick, getTopWickSize(testMax,testOpen,testClose) / pricingScale,marginPrice);
            bottomWickSize = isWithinMargin(mainBottomWick, getBottomWickSize(testMin,testOpen,testClose) / pricingScale,marginPrice);
        }

        if (ignoreWicks) {
            return bodySize;
        } else {
            return bodySize && topWickSize && bottomWickSize;
        }
    }

    //This is MUCH slower
    private boolean areCandlesSimilar(Stock stock, DateTime mainTime, DateTime testTime, float marginPrice, float pricingScale) {
        return areCandlesSimilar(
                new float[]{stock.getOpen(mainTime),stock.getClose(mainTime),stock.getMin(mainTime),stock.getMax(mainTime)},
                new float[]{stock.getOpen(testTime),stock.getClose(testTime),stock.getMin(testTime),stock.getMax(testTime)},
                marginPrice,pricingScale,false);
    }

    private boolean isWithinMargin(float mainValue, float testValue, float margin) {
        return (mainValue + margin >= testValue && mainValue - margin <= testValue);
    }

    private float getTopWickSize(float max, float open, float close) {
        return getOpenCloseDifference(open,close) >= 0 ? max - close : max - open;
    }

    private float getBottomWickSize(float min, float open, float close) {
        return getOpenCloseDifference(open,close) <= 0 ? close - min : open - min;
    }

    private float getOpenCloseDifference(float open, float close) {
        return close - open;
    }
}
