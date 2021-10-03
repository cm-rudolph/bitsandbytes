package de.famiru.bitsandbytes.exactcover;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class PuzzleTest {
    private final Puzzle puzzle = new Puzzle();

    @Test
    void testNoseConstraintColumnIndexIntegrity() {
        Map<Integer, Integer> countPerIdx = new HashMap<>(80);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int i = 0; i < 8; i++) {
                    int idx = puzzle.getNoseConstraintColumnHeadIndex(x, y, i);
                    countPerIdx.merge(idx, 1, Integer::sum);
                }
            }
        }
        int ones = 0;
        int twos = 0;
        for (int i = 0; i < 80; i++) {
            assertThat(countPerIdx).containsKey(i);
            assertThat(countPerIdx.get(i)).isBetween(1, 2);
            if (countPerIdx.get(i).equals(1)) {
                ones++;
            } else {
                twos++;
            }
        }
        assertThat(ones).isEqualTo(32);
        assertThat(twos).isEqualTo(48);
    }

    @Test
    void testRotation() {
        assertThat(puzzle.rotateValue(0b11000000, 0)).isEqualTo(0b11000000);
        assertThat(puzzle.rotateValue(0b11000000, 1)).isEqualTo(0b00110000);
        assertThat(puzzle.rotateValue(0b11000000, 2)).isEqualTo(0b00001100);
        assertThat(puzzle.rotateValue(0b11000000, 3)).isEqualTo(0b00000011);
        assertThat(puzzle.rotateValue(0b00000011, 1)).isEqualTo(0b11000000);
    }
}