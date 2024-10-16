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

    public MatrixEntry<T> getRight() {
        return right;
    }

    public MatrixEntry<T> getColumnHeader() {
        return columnHeader;
    }

    private boolean removeRow() {
        boolean result = true;
        MatrixEntry<T> p = this.right;
        while (p != this) {
            p.upper.lower = p.lower;
            p.lower.upper = p.upper;
            p.columnHeader.rowCount--;
            if (p.columnHeader.rowCount == 0) result = false;
            p = p.right;
        }
        return result;
    }

    private boolean removeCol() {
        boolean result = true;
        columnHeader.left.right = columnHeader.right;
        columnHeader.right.left = columnHeader.left;
        MatrixEntry<T> p = this.lower;
        while (p != this) {
            if (p != columnHeader)
                if (!p.removeRow()) result = false;
            p = p.lower;
        }
        return result;
    }

    boolean removeEntry() {
        boolean result = true;
        MatrixEntry<T> p = this;
        do {
            if (!p.removeCol()) result = false;
            p = p.right;
        } while (p != this);
        return result;
    }

    MatrixEntry<T> getLower() {
        return lower;
    }

    private void reinsertRow() {
        MatrixEntry<T> p = this.right;
        while (p != this) {
            p.upper.lower = p;
            p.lower.upper = p;
            p.columnHeader.rowCount++;
            p = p.right;
        }
    }

    private void reinsertCol() {
        columnHeader.left.right = columnHeader;
        columnHeader.right.left = columnHeader;
        MatrixEntry<T> p = this.lower;
        while (p != this) {
            if (p != columnHeader)
                p.reinsertRow();
            p = p.lower;
        }
    }

    void reinsert() {
        MatrixEntry<T> p = this;
        do {
            p.reinsertCol();
            p = p.right;
        } while (p != this);
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
}
