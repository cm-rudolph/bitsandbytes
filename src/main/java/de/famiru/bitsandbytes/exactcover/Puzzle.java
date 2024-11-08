package de.famiru.bitsandbytes.exactcover;

import de.famiru.dlx.Dlx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Puzzle {
    private static final Logger LOGGER = LogManager.getLogger(Puzzle.class);

    private static final int[] PIECES = new int[]{
            0b11111111, 0b10101010, 0b11111101, 0b11001101,
            0b10110100, 0b10101100, 0b11010111, 0b01011110,
            0b01000100, 0b01010111, 0b00101001, 0b00011000,
            0b11110101, 0b00000000, 0b00010011, 0b10000000
    };
    //private final MatrixEntry<PieceInfo>[] columnHeads; // 80 nose constraints, (80) 16 field constraints,
    // (96) 16 piece used constraints, (112) 1 border stick constraint
    private final Dlx<PieceInfo> dlx;

    public Puzzle() {
        dlx = new Dlx<>(113, getOptionalBorderNoseChoiceIndices(), 1, true, 1000);
        initializeMatrix();
    }

    private static String toBinaryString(int value) {
        String s = Integer.toBinaryString(value);
        while (s.length() < 8) {
            s = "0" + s;
        }
        return s;
    }

    public Collection<Solution> calculateSolution() {
        return List.of(convertSolution(dlx.solve().getFirst()));
    }

    private Solution convertSolution(List<PieceInfo> solution) {
        Entry[] result = new Entry[16];
        for (PieceInfo element : solution) {
            if (element.value() >= 0) {
                int x = element.value() >> 10 & 0b11;
                int y = element.value() >> 12 & 0b11;
                int rotations = element.value() >> 8 & 0b11;
                if (result[x + y * 4] != null) {
                    LOGGER.error("Double entry found!");
                }
                int value = element.value() & 0b11111111;
                int backRotatedValue = rotateValue(value, (4 - rotations) % 4);
                result[x + y * 4] = new Entry(backRotatedValue, rotations);
            }
        }
        return new Solution(result);
    }

    private void initializeMatrix() {
        createPieceChoices();
        createBorderStickChoice();
    }

    private void createPieceChoices() {
        for (int i = 0; i < PIECES.length; i++) {
            int value = PIECES[i];
            createPieceChoicesForValue(value, i);

            int rotatedValue = rotateValue(value, 1);
            if (rotatedValue == value) continue;
            createPieceChoicesForValue(rotatedValue | 1 << 8, i);

            rotatedValue = rotateValue(value, 2);
            if (rotatedValue == value) continue;
            createPieceChoicesForValue(rotatedValue | 2 << 8, i);

            rotatedValue = rotateValue(value, 3);
            createPieceChoicesForValue(rotatedValue | 3 << 8, i);
        }
    }

    int rotateValue(int value, int numberOfRotations) {
        int shift = 2 * numberOfRotations;
        return (value >> shift) | (value << (8 - shift)) & 0b11111111;
    }

    private void createPieceChoicesForValue(int value, int pieceIdx) {
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                createPieceChoiceForValue(value, pieceIdx, x, y);
            }
        }
    }

    private void createPieceChoiceForValue(int value, int pieceIdx, int x, int y) {
        int valueWithLocation = value | x << 10 | y << 12;

        List<Integer> indices = new ArrayList<>();
        indices.add(96 + pieceIdx);
        indices.add(80 + x + y * 4);

        int bitmask = 0b10000000;
        for (int noseIdx = 0; noseIdx < 8; noseIdx++) {
            if ((bitmask & value) != 0) {
                indices.add(getNoseConstraintColumnHeadIndex(x, y, noseIdx));
            }
            bitmask >>= 1;
        }

        indices.sort(Comparator.naturalOrder());
        dlx.addChoice(createPieceInfo(valueWithLocation), indices);
    }

    private void createBorderStickChoice() {
        List<Integer> indices = new ArrayList<>(getBorderStickIndices());
        indices.sort(Comparator.naturalOrder());
        dlx.addChoice(new PieceInfo("border sticks", -1), indices);
    }

    private List<Integer> getBorderStickIndices() {
        return List.of(
                getNoseConstraintColumnHeadIndex(0, 0, 1),
                getNoseConstraintColumnHeadIndex(0, 0, 7),
                getNoseConstraintColumnHeadIndex(3, 0, 1),
                getNoseConstraintColumnHeadIndex(3, 0, 3),
                getNoseConstraintColumnHeadIndex(0, 3, 5),
                getNoseConstraintColumnHeadIndex(0, 3, 7),
                getNoseConstraintColumnHeadIndex(3, 3, 3),
                getNoseConstraintColumnHeadIndex(3, 3, 5),
                112 // "border stick is necessary" column
        );
    }

    int getNoseConstraintColumnHeadIndex(int x, int y, int idx) {
        if (((x + y) & 1) == 0) {
            return ((x + 4 * y) / 2) * 8 + idx;
        } else if (idx <= 1 && y == 0) {
            return 64 + x / 2 * 2 + idx;
        } else if ((idx == 2 || idx == 3) && x == 3) {
            return 68 + y / 2 * 2 + idx - 2;
        } else if ((idx == 4 || idx == 5) && y == 3) {
            return 72 + x / 2 * 2 + idx - 4;
        } else if (idx >= 6 && x == 0) {
            return 76 + y / 2 * 2 + idx - 6;
        } else if (idx == 0) {
            return getNoseConstraintColumnHeadIndex(x, y - 1, 5);
        } else if (idx == 1) {
            return getNoseConstraintColumnHeadIndex(x, y - 1, 4);
        } else if (idx == 2) {
            return getNoseConstraintColumnHeadIndex(x + 1, y, 7);
        } else if (idx == 3) {
            return getNoseConstraintColumnHeadIndex(x + 1, y, 6);
        } else if (idx == 4) {
            return getNoseConstraintColumnHeadIndex(x, y + 1, 1);
        } else if (idx == 5) {
            return getNoseConstraintColumnHeadIndex(x, y + 1, 0);
        } else if (idx == 6) {
            return getNoseConstraintColumnHeadIndex(x - 1, y, 3);
        } else {
            return getNoseConstraintColumnHeadIndex(x - 1, y, 2);
        }
    }

    private Set<Integer> getOptionalBorderNoseChoiceIndices() {
        List<Integer> borderStickIndices = getBorderStickIndices();
        Set<Integer> result = new HashSet<>();

        for (int i = 0; i < 4; i++) {
            addIfNotBorderStickIndex(i, 0, 0, borderStickIndices, result);
            addIfNotBorderStickIndex(i, 0, 1, borderStickIndices, result);
            addIfNotBorderStickIndex(3, i, 2, borderStickIndices, result);
            addIfNotBorderStickIndex(3, i, 3, borderStickIndices, result);
            addIfNotBorderStickIndex(i, 3, 4, borderStickIndices, result);
            addIfNotBorderStickIndex(i, 3, 5, borderStickIndices, result);
            addIfNotBorderStickIndex(0, i, 6, borderStickIndices, result);
            addIfNotBorderStickIndex(0, i, 7, borderStickIndices, result);
        }

        return result;
    }

    private void addIfNotBorderStickIndex(int x, int y, int idx, List<Integer> borderStickIndices,
                                             Set<Integer> optionalBorderNoseConstraintIndices) {
        int constraintIndex = getNoseConstraintColumnHeadIndex(x, y, idx);
        if (!borderStickIndices.contains(constraintIndex)) {
            optionalBorderNoseConstraintIndices.add(constraintIndex);
        }
    }

    private PieceInfo createPieceInfo(int value) {
        String info;
        if (value == -1) {
            info = "Optional";
        } else {
            int x = value >> 10 & 0b11;
            int y = value >> 12 & 0b11;
            int rotations = value >> 8 & 0b11;

            int backRotatedValue = rotateValue(value & 0b11111111, (4 - rotations) % 4);
            info = "x = " + x + "; y = " + y + "; "
                    + rotations + " rotations; value = " + toBinaryString(backRotatedValue);
        }
        return new PieceInfo(info, value);
    }

    public static class Solution {
        private final Entry[] entries;

        public Solution(Entry[] entries) {
            this.entries = entries;
        }

        @Override
        public String toString() {
            String separator = "            |             |             |            \n";
            StringBuilder sb = new StringBuilder(825);
            sb.append(separator);
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    Entry entry = entries[x + y * 4];
                    sb.append(entry.toString());
                    if (x < 3) {
                        sb.append(" | ");
                    }
                }
                sb.append("\n").append(separator);
                if (y < 3) {
                    sb.append("-----------------------------------------------------\n").append(separator);
                }
            }
            return sb.toString();
        }
    }

    public record Entry(int value, int numberOfRotations) {
        @Override
        public String toString() {
            return toBinaryString(value) + " " + numberOfRotations + "x";
        }
    }
}
