package de.famiru.bitsandbytes;

import de.famiru.bitsandbytes.exactcover.Puzzle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        Instant start = Instant.now();
        Puzzle puzzle = new Puzzle();
        Instant initialized = Instant.now();
        Collection<Puzzle.Solution> solutions = puzzle.calculateSolution();
        Instant done = Instant.now();

        LOGGER.info("Initialization took {}", Duration.between(start, initialized));
        LOGGER.info("Solving took {}", Duration.between(initialized, done));
        LOGGER.info("Found {} solutions.", solutions.size());
        int i = 0;
        for (Puzzle.Solution solution : solutions) {
            LOGGER.info("Solution {}:\n{}\n", i, solution);
            i++;
        }
    }
}
