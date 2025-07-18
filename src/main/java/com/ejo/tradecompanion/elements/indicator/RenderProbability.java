package com.ejo.tradecompanion.elements.indicator;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.shape.CircleUI;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.tradecompanion.data.indicator.IndicatorProbability;
import com.ejo.tradecompanion.util.ChartUtil;

public class RenderProbability extends ElementUI {

    private final IndicatorProbability indicator;
    private final DateTime dateTime;

    private final int size;

    public RenderProbability(IndicatorProbability indicator, Vector pos, int size, DateTime dateTime) {
        super(pos, true, true);
        this.indicator = indicator;
        this.size = size;
        this.dateTime = dateTime;
    }

    @Override
    protected void drawElement(Scene scene, Vector mousePos) {
        if (getIndicator().getStock().getOpenTime() == null) return;
        CircleUI circle = new CircleUI(Vector.NULL, ChartUtil.WIDGET_COLOR.alpha(100),size, CircleUI.Type.ULTRA);
        circle.setPos(scene.getSize().getSubtracted(circle.getRadius(),circle.getRadius()));
        circle.draw();
        DateTime calcTime = dateTime;
        float[] results = getIndicator().getData(calcTime);

        String candles = String.valueOf((int)results[0]);
        String plus1Green = String.valueOf(results[1]);
        String plus1Red = String.valueOf(results[2]);
        String plus5AvgChange = String.valueOf(results[3]);
        String plus5Rise = String.valueOf(results[4]);
        String plus5Fall = String.valueOf(results[5]);
        String runTime = String.valueOf(MathE.roundDouble(getIndicator().getPrevCalcRuntime(), 3));//String.valueOf((int)results[6]);

        //DateTime
        QuickDraw.drawTextCentered(calcTime.toString().split(" ")[1],Fonts.getDefaultFont(15),getPos().getSubtracted(0,size * .76),Vector.NULL,ColorE.WHITE);

        //Candle Count
        QuickDraw.drawTextCentered(candles,Fonts.getDefaultFont(15),getPos().getSubtracted(0,size * .9),Vector.NULL,ColorE.WHITE);

        int yInc = (int)(size * .5);
        QuickDraw.drawTextCentered("Next Candle:", Fonts.getDefaultFont(15),getPos().getSubtracted(0,size - yInc),Vector.NULL,ColorE.WHITE);
        yInc += size * .06;
        QuickDraw.drawText(plus1Green + "%",Fonts.getDefaultFont(15),getPos().getSubtracted(size * .6,size - yInc),ColorE.GREEN);
        QuickDraw.drawText(plus1Red + "%",Fonts.getDefaultFont(15),getPos().getSubtracted(-size * .2,size - yInc),ColorE.RED);
        yInc += size * .4;

        QuickDraw.drawTextCentered("Change in " + getIndicator().getPredictionForwardAmount() + " Candles:", Fonts.getDefaultFont(15),getPos().getSubtracted(0,size - yInc),Vector.NULL,ColorE.WHITE);
        yInc += size * .17;

        QuickDraw.drawTextCentered("$" + plus5AvgChange,Fonts.getDefaultFont(15),getPos().getSubtracted(0,size - yInc),Vector.NULL,ColorE.WHITE);
        yInc += size * .1;
        QuickDraw.drawText(plus5Rise + "%",Fonts.getDefaultFont(15),getPos().getSubtracted(size * .6,size - yInc),ColorE.GREEN);
        QuickDraw.drawText(plus5Fall + "%",Fonts.getDefaultFont(15),getPos().getSubtracted(-size * .2,size - yInc),ColorE.RED);
        yInc += size * .44;
        QuickDraw.drawTextCentered(runTime + "s",Fonts.getDefaultFont(15),getPos().getSubtracted(0,size - yInc),Vector.NULL,ColorE.WHITE);
    }

    @Override
    protected void tickElement(Scene scene, Vector mousePos) {

    }

    @Override
    public boolean updateMouseOver(Vector mousePos) {
        return false;
    }

    public IndicatorProbability getIndicator() {
        return indicator;
    }
}
