package honeyroasted.collect.multi;

import java.util.Objects;

public class Triple<L, M, R> {
    private L left;
    private M middle;
    private R right;

    public Triple(L left, M middle, R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    static class Identity<L, M, R> extends Triple<L, M, R> {
        public Identity(L left, M middle, R right) {
            super(left, middle, right);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
            return this.left() == triple.left && this.middle() == triple.middle && this.right() == triple.right;
        }

        @Override
        public int hashCode() {
            return (System.identityHashCode(this.left()) * 31 + System.identityHashCode(this.middle())) * 31 +
                    System.identityHashCode(this.right());
        }
    }

    public static <L, M, R> Triple<L, M, R> of(L left, M middle, R right) {
        return new Triple<>(left, middle, right);
    }

    public static <L, M, R> Triple<L, M, R> identity(L left, M middle, R right) {
        return new Identity<>(left, middle, right);
    }

    public L left() {
        return this.left;
    }

    public M middle() {
        return this.middle;
    }

    public R right() {
        return this.right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
        return Objects.equals(left, triple.left) && Objects.equals(middle, triple.middle) && Objects.equals(right, triple.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, middle, right);
    }

    @Override
    public String toString() {
        return "Triple{" +
                "left=" + left +
                ", middle=" + middle +
                ", right=" + right +
                '}';
    }
}
