package de.famiru.bitsandbytes.exactcover;

class MatrixEntry {
    private final MatrixEntry columnHeader;
    private final String info;
    private MatrixEntry left;
    private MatrixEntry right;
    private MatrixEntry upper;
    private MatrixEntry lower;
    private int value;

    MatrixEntry(String info) {
        value = 0;
        left = this;
        right = this;
        upper = this;
        lower = this;
        columnHeader = this;
        this.info = info;
    }

    MatrixEntry(String info, int value, MatrixEntry columnHeader) {
        left = this;
        right = this;
        upper = this;
        lower = this;
        this.value = value;
        this.columnHeader = columnHeader;
        this.info = info;
    }

    void insertBefore(MatrixEntry entry) {
        entry.right = this;
        entry.left = left;
        left.right = entry;
        left = entry;
    }

    public void insertAbove(MatrixEntry entry) {
        entry.lower = this;
        entry.upper = upper;
        upper.lower = entry;
        upper = entry;
        columnHeader.value++;
    }

    public MatrixEntry getRight() {
        return right;
    }

    public MatrixEntry getColumnHeader() {
        return columnHeader;
    }

    private boolean removeRow() {
        boolean result = true;
        MatrixEntry p = this.right;
        while (p != this) {
            p.upper.lower = p.lower;
            p.lower.upper = p.upper;
            p.columnHeader.value--;
            if (p.columnHeader.value == 0) result = false;
            p = p.right;
        }
        return result;
    }

    private boolean removeCol() {
        boolean result = true;
        columnHeader.left.right = columnHeader.right;
        columnHeader.right.left = columnHeader.left;
        MatrixEntry p = this.lower;
        while (p != this) {
            if (p != columnHeader)
                if (!p.removeRow()) result = false;
            p = p.lower;
        }
        return result;
    }

    boolean removeEntry() {
        boolean result = true;
        MatrixEntry p = this;
        do {
            if (!p.removeCol()) result = false;
            p = p.right;
        } while (p != this);
        return result;
    }

    MatrixEntry getLower() {
        return lower;
    }

    private void reinsertRow() {
        MatrixEntry p = this.right;
        while (p != this) {
            p.upper.lower = p;
            p.lower.upper = p;
            p.columnHeader.value++;
            p = p.right;
        }
    }

    private void reinsertCol() {
        columnHeader.left.right = columnHeader;
        columnHeader.right.left = columnHeader;
        MatrixEntry p = this.lower;
        while (p != this) {
            if (p != columnHeader)
                p.reinsertRow();
            p = p.lower;
        }
    }

    void reinsert() {
        MatrixEntry p = this;
        do {
            p.reinsertCol();
            p = p.right;
        } while (p != this);
    }

    int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return info;
    }
}
