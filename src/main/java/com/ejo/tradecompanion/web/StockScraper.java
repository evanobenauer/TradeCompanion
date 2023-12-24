package com.ejo.tradecompanion.web;

import com.ejo.glowlib.time.DateTime;
import com.ejo.tradecompanion.data.Stock;
import com.ejo.tradecompanion.util.TimeFrame;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;

/*
* POTENTIAL WEBSITES TO SCRAPE FROM
* Daily, Weekly, Monthly Data: https://www.investing.com/etfs/spdr-s-p-500-historictal-data
* 1MIN, 5MIN, 15MIN, 30MIN, 1HR, 5HR:  (Tooltip data is stored in HTML. IF you can find a way to move the mouse across and scan, it should work)
*    https://www.marketwatch.com/investing/fund/spy/charts?mod=mw_quote_advanced
*    https://www.investing.com/etfs/spdr-s-p-500
*    https://medium.com/@rushic24/how-i-webscraped-1-minute-stock-data-from-tradingview-5bc9b4a823a9
*
*
 */

public class StockScraper {

    private static final String WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36";

    private final Stock stock;

    public StockScraper(Stock stock) {
        this.stock = stock;
    }


    public float scrapeLivePrice(PriceSource priceSource) throws IOException {
        float livePrice;
        switch (priceSource) {
            case MARKETWATCH -> {
                String url = "https://www.marketwatch.com/investing/fund/" + getStock().getTicker();
                livePrice = scrapeLivePrice(url, "bg-quote.value", 0);
            }
            case YAHOOFINANCE -> {
                String url2 = "https://finance.yahoo.com/quote/" + getStock().getTicker() + "?p=" + getStock().getTicker();
                livePrice = scrapeLivePrice(url2, "data-test", "qsp-price", 0);
            }
            default -> livePrice = -1;
        }
        return livePrice;
    }

    public float[] scrapeLivePrices(PriceSource priceSource,Stock... stocks) {
        //Add this and have it scrape data from a website with multiple stocks on one connection so I don't overload
        return null;
    }

    public HashMap<Long, float[]> scrapeHistoricalData(TimeFrame timeFrame, DateTime startTime, DateTime endTime) {
        //TODO: Add this. With it, I will be capable of running multiple stocks at one so that I may trade many at a time with the same strategy on a bot
        return null;
    }


    private static float scrapeLivePrice(String url, String attributeKey, String attributeValue, int priceIndex) throws IOException {
        try {
            Document doc = Jsoup.connect(url).userAgent(WEB_USER_AGENT).timeout(5 * 1000).get();
            Elements cssElements = doc.getElementsByAttributeValue(attributeKey, attributeValue);
            String priceString = cssElements.get(priceIndex).text().replace("$", "");
            return priceString.equals("") ? -1 : Float.parseFloat(priceString);
        } catch (IndexOutOfBoundsException e) {
            return -1;
        }
    }

    private static float scrapeLivePrice(String url, String cssQuery, int priceIndex) throws IOException {
        try {
            Document doc = Jsoup.connect(url).userAgent(WEB_USER_AGENT).timeout(5 * 1000).get();
            Elements cssElements = doc.select(cssQuery);
            String priceString = cssElements.get(priceIndex).text().replace("$", "");
            return priceString.equals("") ? -1 : Float.parseFloat(priceString);
        } catch (IndexOutOfBoundsException e) {
            return -1;
        }
    }


    public Stock getStock() {
        return stock;
    }

    public enum PriceSource {
        MARKETWATCH("MarketWatch"),
        YAHOOFINANCE("YahooFinance");

        private final String string;

        PriceSource(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        @Override
        public String toString() {
            return getString();
        }
    }

}
