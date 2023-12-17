package com.ejo.tradecompanion.data;

import com.ejo.glowlib.file.FileManager;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

public abstract class HistoricalDataContainer {

    protected HashMap<Long, float[]> dataHash = new HashMap<>();

    private final Container<Double> progressContainer = new Container<>(0d);

    protected boolean progressActive = false;

    /**
     * This method loads all historical data saved in the data directory. It converts the key information of the hashmap data into a long to be used in development
     * @return
     */
    public HashMap<Long, float[]> loadHistoricalData(String filePath, String fileName) {
        getProgressContainer().set(0d);
        this.progressActive = true;
        try {
            File file = new File(filePath + (fileName.equals("") ? "" : "/") + fileName.replace(".csv", "") + ".csv");
            HashMap<Long, float[]> rawMap = new HashMap<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                long fileSize = Files.lines(file.toPath()).count();
                long currentRow = 0;
                while ((line = reader.readLine()) != null) {
                    String[] row = line.split(",");
                    long key = Long.parseLong(row[0]);
                    String[] rowCut = line.replace(key + ",", "").split(",");

                    float[] floatRowCut = new float[rowCut.length];
                    for (int i = 0; i < rowCut.length; i++) floatRowCut[i] = Float.parseFloat(rowCut[i]);

                    rawMap.put(key, floatRowCut);
                    currentRow += 1;
                    getProgressContainer().set((double) currentRow / fileSize);
                }
            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }
            this.progressActive = false;
            return this.dataHash = rawMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.progressActive = false;
        return new HashMap<>();
    }

    public HashMap<Long, float[]> loadHistoricalData() {
        return loadHistoricalData(getDefaultFilePath(), getDefaultFileName());
    }

    /**
     * This method saves all historical data from the HashMap as a CSV file using GlowLib
     * @return
     */
    public boolean saveHistoricalData(String filePath, String fileName) {
        getProgressContainer().set(0d);
        this.progressActive = true;
        FileManager.createFolderPath(filePath); //Creates the folder path if it does not exist
        HashMap<Long, float[]> hashMap = getHistoricalData();
        String outputFile = filePath + (filePath.equals("") ? "" : "/") + fileName.replace(".csv","") + ".csv";
        long fileSize = hashMap.size();
        long currentRow = 0;
        try(FileWriter writer = new FileWriter(outputFile)) {
            for (Long key : hashMap.keySet()) {
                writer.write(key + "," + Arrays.toString(hashMap.get(key)).replace("[","").replace("]","").replace(" ","") + "\n");
                currentRow += 1;
                getProgressContainer().set((double) currentRow / fileSize);
            }
            this.progressActive = false;
            return true;
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
        this.progressActive = false;
        return false;
    }


    public boolean saveHistoricalData() {
        return saveHistoricalData(getDefaultFilePath(), getDefaultFileName());
    }


    /**
     * Loads all historical data from a file and applies it on top of the current historical data. It will overwrite data if it exists,
     * but keep data that does not have a key present from the loaded data. This can be used mainly to keep live data and apply an updated
     * historical data to that live data set
     * @param filePath
     * @param fileName
     * @return
     */
    public void applyLoadHistoricalData(HashMap<Long,float[]> historicalData, String filePath, String fileName) {
        getProgressContainer().set(0d);
        this.progressActive = true;
        try {
            File file = new File(filePath + (fileName.equals("") ? "" : "/") + fileName.replace(".csv", "") + ".csv");

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                long fileSize = Files.lines(file.toPath()).count();
                long currentRow = 0;
                while ((line = reader.readLine()) != null) {
                    String[] row = line.split(",");
                    long key = Long.parseLong(row[0]);
                    String[] rowCut = line.replace(key + ",", "").split(",");

                    float[] floatRowCut = new float[rowCut.length];
                    for (int i = 0; i < rowCut.length; i++) floatRowCut[i] = Float.parseFloat(rowCut[i]);

                    historicalData.put(Long.parseLong(row[0]), floatRowCut);
                    currentRow += 1;
                    getProgressContainer().set((double) currentRow / fileSize);
                }
            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.progressActive = false;
    }

    public void applyLoadHistoricalData() {
        applyLoadHistoricalData(getHistoricalData(),getDefaultFilePath(),getDefaultFileName());
    }

    public boolean applySaveHistoricalData() {
        //TODO: Load in all data from the file, overwrite if necessary. Then save all the data
        // Then unload all data. Then reload from the specific timeframe
        return saveHistoricalData(getDefaultFilePath(), getDefaultFileName());
    }



    //TODO: Create a "Load Historical Data" with timeframe parameters as not to load the whole thing so memory doesn't explode
    // This would cause issues saving. So if the data is not the full data,
    // reload the data file using "Apply", then save it all
    // Then reload the data from the timeframe as not to store the old data. Then call System.gc()

    public HashMap<Long, float[]> loadHistoricalData(String filePath, String fileName, DateTime startTime, DateTime endTime) {
        return null;
    }

    public HashMap<Long, float[]> loadHistoricalData(DateTime startTime, DateTime endTime) {
        return loadHistoricalData(getDefaultFilePath(),getDefaultFileName());
    }

    public abstract float[] getData(DateTime dateTime);


    public abstract String getDefaultFileName();

    public abstract String getDefaultFilePath();



    public Container<Double> getProgressContainer() {
        return progressContainer;
    }

    public boolean isProgressActive() {
        return progressActive;
    }


    public HashMap<Long, float[]> getHistoricalData() {
        return dataHash;
    }
}
