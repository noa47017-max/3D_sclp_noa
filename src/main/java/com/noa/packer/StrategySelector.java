package com.noa.packer;
import java.util.Random;

public class StrategySelector {
    private Random rand = new Random();

    public boolean shouldRunGreedy(int currentIteration, int maxIterations) {
        double rTau = rand.nextDouble(); // R(τ) [5]

        // חישוב P(τ) לפי התפלגות נורמלית סטנדרטית (משוואה 12)
        double pTau = calculateNormalProbability(currentIteration, maxIterations);
        // החלטה: GSM אם R(τ) - P(τ) <= 0, אחרת ASM [5]
        return (rTau - pTau) <= 0;
    }

    private double calculateNormalProbability(int tau, int M) {
        /**
         * לפי המאמר: משתמשים בצד הימני של ההתפלגות.
         * כדי שאיטרציה 0 תהיה "מרכז" ההתפלגות (הסתברות מקסימלית ל-GSM),
         * נגדיר את הממוצע (mu) להיות 0.
         */
        double mu = 0;
        double sigma = 1.0; // לפי המאמר [1]
        // נרמול ה-tau לטווח שבין 0 ל-3 (כדי לכסות את רוב שטח ההתפלגות הנורמלית)
        double x = (double) tau / M * 3.0;
        return Math.exp(-Math.pow(x - mu, 2) / (2.0 * Math.pow(sigma, 2)));
    }
}
