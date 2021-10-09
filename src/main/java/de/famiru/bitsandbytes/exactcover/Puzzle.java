package de.famiru.bitsandbytes.exactcover;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class Puzzle {
    private static final Logger LOGGER = LogManager.getLogger(Puzzle.class);

    private static final int[] PIECES = new int[]{
            0b11111111, 0b10101010, 0b11111101, 0b11001100,
            0b10110100, 0b10101100, 0b01111101, 0b01011110,
            0b01000100, 0b01110101, 0b10010010, 0b00011000,
            0b11110101, 0b00000000, 0b00010011, 0b00001000
    };
    private final MatrixEntry head;
    private final MatrixEntry[] columnHeads; // 80 nose constraints, (80) 16 field constraints,
    // (96) 16 piece used constraints, (112) 1 border stick constraint
    private final FutureTask<Collection<Solution>> futureTask;

    public Puzzle() {
        futureTask = new FutureTask<>(this::solve);
        head = new MatrixEntry("Head");
        columnHeads = new MatrixEntry[113];
        initializeMatrix();
    }

    private static String toBinaryString(int value) {
        String s = Integer.toBinaryString(value);
        while (s.length() < 8) {
            s = "0" + s;
        }
        return s;
    }

    public Future<Collection<Solution>> calculateSolution() {
        if (!futureTask.isDone()) {
            new Thread(futureTask).start();
        }
        return futureTask;
    }

    private Collection<Solution> solve() {
        List<Solution> result = new ArrayList<>();
        List<MatrixEntry> solution = new ArrayList<>(PIECES.length);
        MatrixEntry p = selectNextColumn();
        while (head.getRight() != head) {
            p = p.getLower();
            if (p.getColumnHeader() != p) {
                solution.add(p);
                if (p.removeEntry()) {
                    p = selectNextColumn();
                } else {
                    p = backtrack(solution);
                }
            } else {
                if (solution.isEmpty()) {
                    break;
                }
                p = backtrack(solution);
            }
            if (head.getRight() == head) {
                result.add(convertSolution(solution));
                if (result.size() % 10 == 0) {
                    LOGGER.info("Found {} solutions so far.", result.size());
                }
                p = backtrack(solution);
            }
        }

        return List.copyOf(result);
    }

    private Solution convertSolution(List<MatrixEntry> solution) {
        Entry[] result = new Entry[16];
        for (MatrixEntry matrixEntry : solution) {
            if (matrixEntry.getValue() >= 0) {
                int x = matrixEntry.getValue() >> 10 & 0b11;
                int y = matrixEntry.getValue() >> 12 & 0b11;
                int rotations = matrixEntry.getValue() >> 8 & 0b11;
                if (result[x + y * 4] != null) {
                    LOGGER.error("Double entry found!");
                }
                int value = matrixEntry.getValue() & 0b11111111;
                int backRotatedValue = rotateValue(value, (4 - rotations) % 4);
                result[x + y * 4] = new Entry(backRotatedValue, rotations);
            }
        }
        return new Solution(result);
    }

    private MatrixEntry backtrack(List<MatrixEntry> solution) {
        MatrixEntry entry = solution.remove(solution.size() - 1);
        entry.reinsert();

        return entry;
    }

    private void initializeMatrix() {
        createConstraintColumnHeads();

        createPieceChoices();
        createBorderStickChoice();
        createOptionalBorderNoseChoices();
        //createOptionalInnerNoseChoices();
    }

    private void createConstraintColumnHeads() {
        for (int i = 0; i < columnHeads.length; i++) {
            String info;
            if (i < 80) {
                info = "Nose " + i;
            } else if (i < 96) {
                info = "Field " + (i - 80) + " used";
            } else if (i < 112) {
                info = "Piece " + (i - 96) + " placed";
            } else {
                info = "Border sticks";
            }
            columnHeads[i] = new MatrixEntry(info);
            head.insertBefore(columnHeads[i]);
        }
    }

    private void createPieceChoices() {
        for (int i = 0; i < PIECES.length; i++) {
            int value = PIECES[i];
            createPieceChoicesForValue(value, i);

            /*int rotatedValue = rotateValue(value, 1);
            if (rotatedValue == value) continue;
            createPieceChoicesForValue(rotatedValue | 1 << 8, i);

            rotatedValue = rotateValue(value, 2);
            if (rotatedValue == value) continue;
            createPieceChoicesForValue(rotatedValue | 2 << 8, i);

            rotatedValue = rotateValue(value, 3);
            createPieceChoicesForValue(rotatedValue | 3 << 8, i);*/
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
        MatrixEntry pieceUsedColumnHead = columnHeads[96 + pieceIdx];
        MatrixEntry firstEntry = createMatrixEntry(pieceUsedColumnHead, null, valueWithLocation);

        MatrixEntry fieldUsedColumnHead = columnHeads[80 + x + y * 4];
        createMatrixEntry(fieldUsedColumnHead, firstEntry, valueWithLocation);

        int bitmask = 0b10000000;
        for (int noseIdx = 0; noseIdx < 8; noseIdx++) {
            if ((bitmask & value) != 0) {
                createMatrixEntry(getNoseConstraintColumnHead(x, y, noseIdx), firstEntry, valueWithLocation);
            }
            bitmask >>= 1;
        }
    }

    private void createBorderStickChoice() {
        // border sticks are necessary
        MatrixEntry firstEntry = createMatrixEntry(columnHeads[112], null);
        createMatrixEntry(getNoseConstraintColumnHead(0, 0, 1), firstEntry);
        createMatrixEntry(getNoseConstraintColumnHead(0, 0, 7), firstEntry);
        createMatrixEntry(getNoseConstraintColumnHead(3, 0, 1), firstEntry);
        createMatrixEntry(getNoseConstraintColumnHead(3, 0, 3), firstEntry);
        createMatrixEntry(getNoseConstraintColumnHead(0, 3, 5), firstEntry);
        createMatrixEntry(getNoseConstraintColumnHead(0, 3, 7), firstEntry);
        createMatrixEntry(getNoseConstraintColumnHead(3, 3, 3), firstEntry);
        createMatrixEntry(getNoseConstraintColumnHead(3, 3, 5), firstEntry);
    }

    private MatrixEntry getNoseConstraintColumnHead(int x, int y, int idx) {
        return columnHeads[getNoseConstraintColumnHeadIndex(x, y, idx)];
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

    private void createOptionalBorderNoseChoices() {
        // simply create all optional border nose choices - superfluous ones get removed at first step through
        // border stick constraint
        for (int i = 0; i < 4; i++) {
            createMatrixEntry(getNoseConstraintColumnHead(i, 0, 0), null);
            createMatrixEntry(getNoseConstraintColumnHead(i, 0, 1), null);
            createMatrixEntry(getNoseConstraintColumnHead(3, i, 2), null);
            createMatrixEntry(getNoseConstraintColumnHead(3, i, 3), null);
            createMatrixEntry(getNoseConstraintColumnHead(i, 3, 4), null);
            createMatrixEntry(getNoseConstraintColumnHead(i, 3, 5), null);
            createMatrixEntry(getNoseConstraintColumnHead(0, i, 6), null);
            createMatrixEntry(getNoseConstraintColumnHead(0, i, 7), null);
        }
    }

    private void createOptionalInnerNoseChoices() {
        Set<MatrixEntry> allHeads = new HashSet<>(48);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                if (y > 0) {
                    createMatrixEntryIfNotPresent(allHeads, x, y, 0);
                    createMatrixEntryIfNotPresent(allHeads, x, y, 1);
                }
                if (x < 3) {
                    createMatrixEntryIfNotPresent(allHeads, x, y, 2);
                    createMatrixEntryIfNotPresent(allHeads, x, y, 3);
                }
                if (y < 3) {
                    createMatrixEntryIfNotPresent(allHeads, x, y, 4);
                    createMatrixEntryIfNotPresent(allHeads, x, y, 5);
                }
                if (x > 0) {
                    createMatrixEntryIfNotPresent(allHeads, x, y, 6);
                    createMatrixEntryIfNotPresent(allHeads, x, y, 7);
                }
            }
        }
    }

    private void createMatrixEntryIfNotPresent(Set<MatrixEntry> presentHeads, int x, int y, int idx) {
        MatrixEntry head = getNoseConstraintColumnHead(x, y, idx);
        if (presentHeads.add(head)) {
            createMatrixEntry(head, null);
        }
    }

    private MatrixEntry createMatrixEntry(MatrixEntry columnHead, MatrixEntry choice) {
        return createMatrixEntry(columnHead, choice, -1);
    }

    private MatrixEntry createMatrixEntry(MatrixEntry columnHead, MatrixEntry choice, int value) {
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
        MatrixEntry e = new MatrixEntry(info, value, columnHead);
        columnHead.insertAbove(e);
        if (choice != null) {
            choice.insertBefore(e);
        }

        return e;
    }

    private MatrixEntry selectNextColumn() {
        MatrixEntry p = head.getRight();
        MatrixEntry bestMatch = p;
        int bestRowCount = p.getValue();
        while (p != head && bestRowCount > 1) {
            if (p.getValue() < bestRowCount) {
                bestRowCount = p.getValue();
                bestMatch = p;
            }
            p = p.getRight();
        }
        return bestMatch;
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
