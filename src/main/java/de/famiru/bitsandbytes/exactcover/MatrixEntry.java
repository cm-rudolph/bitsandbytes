package de.famiru.bitsandbytes.exactcover;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

class MatrixEntry<T> {
    private final T data;
    private final MatrixEntry<T> columnHeader;
    private MatrixEntry<T> left;
    private MatrixEntry<T> right;
    private MatrixEntry<T> upper;
    private MatrixEntry<T> lower;
    private int rowCount;

    // constructor for column header entries
    MatrixEntry() {
        rowCount = 0;
        left = this;
        right = this;
        upper = this;
        lower = this;
        columnHeader = this;
        this.data = null;
    }

    // constructor for regular entries
    MatrixEntry(T data, MatrixEntry<T> columnHeader) {
        left = this;
        right = this;
        upper = this;
        lower = this;
        this.columnHeader = requireNonNull(columnHeader);
        this.data = requireNonNull(data);
    }

    void insertBefore(MatrixEntry<T> entry) {
        entry.right = this;
        entry.left = left;
        left.right = entry;
        left = entry;
    }

    public void insertAbove(MatrixEntry<T> entry) {
        entry.lower = this;
        entry.upper = upper;
        upper.lower = entry;
        upper = entry;
        columnHeader.rowCount++;
    }

    public MatrixEntry<T> getLeft() {
        return left;
    }

    public MatrixEntry<T> getRight() {
        return right;
    }

    MatrixEntry<T> getLower() {
        return lower;
    }

    int getRowCount() {
        return rowCount;
    }

    T getData() {
        return data;
    }

    @Override
    public String toString() {
        return Objects.requireNonNullElse(data, "Head").toString();
    }

    public void coverColumn() {
        columnHeader.right.left = columnHeader.left;
        columnHeader.left.right = columnHeader.right;
        MatrixEntry<T> i = columnHeader.lower;
        while (i != columnHeader) {
            MatrixEntry<T> j = i.right;
            while (j != i) {
                j.lower.upper = j.upper;
                j.upper.lower = j.lower;
                j.columnHeader.rowCount--;
                j = j.right;
            }
            i = i.lower;
        }
    }

    public void uncoverColumn() {
        MatrixEntry<T> i = columnHeader.upper;
        while (i != columnHeader) {
            MatrixEntry<T> j = i.left;
            while (j != i) {
                j.columnHeader.rowCount++;
                j.lower.upper = j;
                j.upper.lower = j;
                j = j.left;
            }
            i = i.upper;
        }
        columnHeader.right.left = columnHeader;
        columnHeader.left.right = columnHeader;
    }
}
