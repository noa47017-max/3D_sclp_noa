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
    private final ThreadPoolTaskExecutor executor;

    @Autowired
    public HybridLoadingSystem(@Qualifier("antAlgorithmExecutor") ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
        LoadingContext ctx = new LoadingContext(cargoTypes);
        LoadingChain bestGlobalSolution = null;
        for (int tau = 0; tau < Global.M; tau++) {
            // הרצת הנמלים
            LoadingChain bestIterationSolution = executeIteration(ctx, container, cargoTypes, tau);
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
        return bestGlobalSolution;
    }

    private LoadingChain executeIteration(LoadingContext ctx, Container container,
                                          List<CargoType> cargoTypes, int tau) {

        List<Future<LoadingChain>> futures = new ArrayList<>();

        for (int antId = 0; antId < Global.m; antId++) {
            futures.add(executor.submit(() -> {
                List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
                // engine חדש לכל נמלה כי StrategySelector הוא per-thread
                AntLoadingEngine engine = new AntLoadingEngine(ctx, container, antCargoes, tau);
                return engine.executeAntLoading();
            }));
        }

        LoadingChain bestIterationSolution = null;
        for (Future<LoadingChain> future : futures) {
            try {
                LoadingChain result = future.get();
                if (isBetterSolution(result, bestIterationSolution)) {
                    bestIterationSolution = result;
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Ant execution failed", e);
            }
        }
        return bestIterationSolution;
    }

    private List<CargoType> deepCopyCargoes(List<CargoType> original) {
        List<CargoType> copy = new ArrayList<>();
        for (CargoType ct : original) copy.add(new CargoType(ct));
        return copy;
    }

    private void reinforcePheromoneFromSolution(LoadingContext ctx,LoadingChain solution, int tau) {
        // בדוק שלא יש null
        if (solution == null || solution.getSteps() == null) {
            return;
        }
        double globalDelta =ctx.pheromones().getRho() * (solution.getVUR()/2 - ctx.pheromones().getTau0());
        // עבור כל צעד בשרשרת הטעינה
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