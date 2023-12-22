package com.ejo.tradecompanion.auto;

import com.ejo.tradecompanion.data.Stock;

public class Trade {

    private final Stock stock;

    private final Type type;
    private final float startPrice;
    private float priceStopLoss;
    private float priceTakeProfit;

    public Trade(Stock stock, Type type, float startPrice, float priceStopLoss, float priceTakeProfit) {
        this.stock = stock;
        this.type = type;
        this.startPrice = startPrice;
        this.priceStopLoss = priceStopLoss;
        this.priceTakeProfit = priceTakeProfit;
    }

    public void update() {
        boolean triggerStopLoss = getStock().getPrice() <= getPriceStopLoss();
        boolean triggerTakeProfit = getStock().getPrice() >= getPriceTakeProfit();
        if (triggerStopLoss || triggerTakeProfit) exit();
    }


    public Trade enter() {
        return this;
    }

    public Trade exit() {
        return this;
    }

    public Trade setTakeProfit(float price) {
        this.priceTakeProfit = price;
        return this;
    }

    public Trade setStopLoss(float price) {
        this.priceStopLoss = price;
        return this;
    }


    public Stock getStock() {
        return stock;
    }

    public Type getType() {
        return type;
    }

    public float getStartPrice() {
        return startPrice;
    }

    public float getPriceStopLoss() {
        return priceStopLoss;
    }

    public float getPriceTakeProfit() {
        return priceTakeProfit;
    }

    public enum Type {
        LONG,
        SHORT
    }

}
