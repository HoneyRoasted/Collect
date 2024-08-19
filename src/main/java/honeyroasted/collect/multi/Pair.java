package honeyroasted.collect.multi;

import java.util.Objects;

public class Pair<L, R> {
    private L left;
    private R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    static class Identity<L, R> extends Pair<L, R> {

        public Identity(L left, R right) {
            super(left, right);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return this.left() == pair.left && this.right() == pair.right;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.left()) * 31 + System.identityHashCode(this.right());
        }
    }

    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    public static <L, R> Pair<L, R> identity(L left, R right) {
        return new Identity<>(left, right);
    }

    public L left() {
        return this.left;
    }

    public R right() {
        return this.right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(left, pair.left) && Objects.equals(right, pair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return "Pair{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }
}
