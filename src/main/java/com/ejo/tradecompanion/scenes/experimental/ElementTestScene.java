package com.ejo.tradecompanion.scenes.experimental;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.scene.Scene;
import com.ejo.tradecompanion.elements.ListDisplayUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ElementTestScene extends Scene {

    ArrayList<String> values = new ArrayList<>(List.of("String1","String2","String3"));

    private final ListDisplayUI<String> listDisplayUI = new ListDisplayUI<>(new Vector(100,100),values);

    public ElementTestScene() {
        super("");
        Random random = new Random();
        for (int i = 0; i < 30; i++) {
            values.add(String.valueOf(random.nextInt(100, 500)));
        }
        addElements(listDisplayUI);
    }
}
