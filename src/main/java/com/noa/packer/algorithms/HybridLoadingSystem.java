package com.noa.packer.algorithms;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.noa.packer.models.Container;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
@Service
public class HybridLoadingSystem {

    //  משאבים משותפים
    private static final Logger logger = LoggerFactory.getLogger(HybridLoadingSystem.class);
    private final Executor antAlgorithmExecutor;

    public HybridLoadingSystem(@Qualifier("antAlgorithmExecutor") Executor antAlgorithmExecutor) {
        this.antAlgorithmExecutor = antAlgorithmExecutor;
    }

    public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
        LoadingContext ctx = new LoadingContext(cargoTypes);
        LoadingChain bestGlobalSolution = null;
        for (int tau = 0; tau < Global.M; tau++) {
            final int iteration = tau;
            //  הרצת הנמלים במקביל
            List<CompletableFuture<LoadingChain>> antTasks = new ArrayList<>(Global.m);

            for (int i = 0; i < Global.m; i++) {
                CompletableFuture<LoadingChain> antTask = CompletableFuture.supplyAsync(() -> {
                    // יצירת עותק לנתונים כדי למנוע התנגשויות בין התהליכונים
                    List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
                    AntLoadingEngine engine = new AntLoadingEngine(ctx, container, antCargoes, iteration);
                    return engine.executeAntLoading();

                }, antAlgorithmExecutor); // שימוש ב-Pool המהיר של האלגוריתם

                antTasks.add(antTask);
            }

            //  איסוף התוצאות מכל הנמלים ומציאת הפתרון הטוב ביותר
            LoadingChain bestIterationSolution = null;
            for (CompletableFuture<LoadingChain> task : antTasks) {
                try {
                    LoadingChain result = task.join();
                    if (isBetterSolution(result, bestIterationSolution)) {
                        bestIterationSolution = result;
                    }
                } catch (CompletionException e) {
                    logger.error("Ant failed in iteration {}", iteration, e.getCause());
                }
            }
            if (bestIterationSolution != null) {
                //  אידוי פרומונים
                ctx.pheromones().applyGlobalEvaporation();
                //  חיזוק פרומונים
                reinforcePheromoneFromSolution(ctx, bestIterationSolution, tau);
                bestGlobalSolution = updateGlobalBestIfNeeded(bestGlobalSolution, bestIterationSolution, tau);
            }
        }
        // סיום
        outputResults(bestGlobalSolution);
        ctx.pheromones().reset();
        ctx.pruning().reset();
        return bestGlobalSolution;
    }

    private List<CargoType> deepCopyCargoes(List<CargoType> original) {
        List<CargoType> copy = new ArrayList<>();
        for (CargoType ct : original) copy.add(new CargoType(ct));
        return copy;
    }

    private void reinforcePheromoneFromSolution(LoadingContext ctx,LoadingChain solution, int tau) {
        if (solution == null || solution.getSteps() == null) {
            return;
        }
        double globalDelta =ctx.pheromones().getRho() * (solution.getVUR()/2 - ctx.pheromones().getTau0());
        for (LoadingChain.PlacedBoxInfo step : solution.getSteps()) {
            // חזק את הפרומון של בחירה זו
            ctx.pheromones().updateGlobalPheromone(
                    step.stateKey,
                    step.cargoType,
                    step.direction,
                    step.leftBackBottom[0],
                    step.leftBackBottom[1],
                    step.leftBackBottom[2],
                    step.rightFrontTop[0],
                    step.rightFrontTop[1],
                    step.rightFrontTop[2],
                    step.remainingSpace,
                    globalDelta // כמות חיזוק פרופורציונלית ל-VUR
            );
        }
    }


    private LoadingChain updateGlobalBestIfNeeded(
            LoadingChain current,
            LoadingChain newSolution,
            int tau) {

        if (isBetterSolution(newSolution, current)) {
            logger.info("Iteration " + tau + ": New Global Best VUR: " +
                            String.format("%.2f", newSolution.getVUR() * 100) + "%");
            return newSolution;
        }
        return current;
    }


    private boolean isBetterSolution(LoadingChain solution1, LoadingChain solution2) {
        if (solution1 == null) {
            return false;
        }
        if (solution2 == null) {
            return true;
        }
        // שניהם לא null - השווה לפי VUR
        return solution1.getVUR() > solution2.getVUR();
    }


    private void outputResults(LoadingChain solution) {
        if (solution != null) {
            System.out.println("===== Final Results =====");
            System.out.println("VUR: " + String.format("%.2f%%", solution.getVUR() * 100));
            System.out.println("Boxes Loaded: " + solution.getSteps().size());
        } else {
            System.out.println("No solution found!");
        }
    }

}