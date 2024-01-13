package com.ejo.tradecompanion.elements;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.util.input.Mouse;
import com.ejo.glowui.util.render.Fonts;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Map;

public class ListDisplayUI<T> extends ElementUI {

    private ArrayList<T> list;

    private ArrayList<TextUI> textList = new ArrayList<>();

    private int fontSize;

    public ListDisplayUI(Vector pos, ArrayList<T> list) {
        super(pos, true, true);
        this.list = list;
        this.fontSize = 40;
    }

    @Override
    protected void drawElement(Scene scene, Vector mousePos) {
        for (TextUI text : textList) {
            Map attributes;
            if (isCenteredTextHovered(text, mousePos)) {
                text.setModifier(3);
                attributes = text.getFont().getAttributes();
                attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
            } else {
                text.setModifier(0);
                attributes = text.getFont().getAttributes();
                attributes.put(TextAttribute.STRIKETHROUGH, false);
            }
            text.setFont(new Font(attributes));
            text.drawCentered(scene,mousePos,Vector.NULL);
        }
    }

    @Override
    protected void tickElement(Scene scene, Vector mousePos) {
        try {
            Font font = Fonts.getDefaultFont(fontSize);
            ArrayList<TextUI> texts = new ArrayList<>();
            double add = 0;
            for (T item : getList()) {
                String str = String.valueOf(item);
                TextUI text;
                texts.add(text = new TextUI(str, font, getPos().getAdded(2, add), ColorE.WHITE));
                add += text.getHeight() + 1;
            }
            this.textList = texts;
        } catch (ConcurrentModificationException ignored) {
        }
    }

    @Override
    public void onMouseClick(Scene scene, int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(scene, button, action, mods, mousePos);

        if (button == Mouse.BUTTON_LEFT.getId() && action == Mouse.ACTION_CLICK) {
            for (TextUI text : textList) {
                if (isCenteredTextHovered(text, mousePos)) {
                    int i = textList.indexOf(text);
                    list.remove(i);
                    System.gc();
                    break;
                }
            }
        }
    }

    @Override
    public boolean updateMouseOver(Vector mousePos) {
        return false;
    }

    private boolean isCenteredTextHovered(TextUI text, Vector mousePos) {
        boolean hoveredX = mousePos.getX() > text.getPos().getX() - text.getWidth() / 2 && mousePos.getX() < text.getPos().getX() + text.getWidth() / 2;
        boolean hoveredY = mousePos.getY() > text.getPos().getY() - text.getHeight() / 2 && mousePos.getY() < text.getPos().getY() + text.getHeight() / 2;
        return hoveredX && hoveredY;
    }

    private boolean isTextHovered(TextUI text, Vector mousePos) {
        boolean hoveredX = mousePos.getX() > text.getPos().getX() && mousePos.getX() < text.getPos().getX() + text.getWidth();
        boolean hoveredY = mousePos.getY() > text.getPos().getY() && mousePos.getY() < text.getPos().getY() + text.getHeight();
        return hoveredX && hoveredY;
    }


    public ListDisplayUI<T> setFontSize(int fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public double getHeight() {
        double height = 0;
        for (TextUI text : textList) {
            height += text.getHeight() + 1;
        }
        return height;
    }


    public ArrayList<T> getList() {
        return list;
    }

    public void setList(ArrayList<T> list) {
        this.list = list;
    }
}
