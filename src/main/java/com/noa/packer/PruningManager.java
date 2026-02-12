package com.noa.packer;
import java.util.TreeMap;

public class PruningManager {
    // מפתח: מילת מצב בינארית, ערך: שרשרת הטעינה הטובה ביותר לאותו מצב [12, 18]
    private TreeMap<String, LoadingChain> pruningMatrix = new TreeMap<>();

    public boolean shouldPrune(String stateWord, double currentVUR) {
        if (pruningMatrix.containsKey(stateWord)) {
            // השוואה: אם המצב הקיים בעץ טוב יותר, גזום את הנמלה הנוכחית [19, 20]
            return currentVUR < pruningMatrix.get(stateWord).getVUR();
        }
        return false;
    }

    public void updateMatrix(String stateWord, LoadingChain chain) {
        pruningMatrix.put(stateWord, chain);
    }
}
