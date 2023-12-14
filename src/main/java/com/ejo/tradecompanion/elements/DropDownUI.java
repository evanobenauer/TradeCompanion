package com.ejo.tradecompanion.elements;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;

import java.util.ArrayList;

public class DropDownUI<T> extends ElementUI {

    private ArrayList<T> list;

    public DropDownUI(Vector pos, ArrayList<T> list) {
        super(pos, true, true);
        this.list = list;
    }

    @Override
    protected void drawElement(Scene scene, Vector mousePos) {

    }

    @Override
    protected void tickElement(Scene scene, Vector mousePos) {
    }

    @Override
    public void onMouseClick(Scene scene, int button, int action, int mods, Vector mousePos) {
        super.onMouseClick(scene, button, action, mods, mousePos);
    }

    @Override
    public boolean updateMouseOver(Vector mousePos) {
        return false;
    }


    public ArrayList<T> getList() {
        return list;
    }

    public void setList(ArrayList<T> list) {
        this.list = list;
    }
}
