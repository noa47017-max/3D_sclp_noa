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
        // פישוט מתמטי של פונקציית ההסתברות המתוארת במאמר [5]
        return Math.exp(-Math.pow(tau - (double)M/2, 2) / 2.0);
    }
}
