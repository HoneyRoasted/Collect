package honeyroasted.collect.multi;

public class Choice<L, R> extends Pair<L, R> {

    protected Choice(L left, R right) {
        super(left, right);
    }

    public static <L, R> Choice<L, R> left(L left) {
        return new Choice<>(left, null);
    }

    public static <L, R> Choice<L, R> right(R right) {
        return new Choice<>(null, right);
    }

    public static <L, R> Choice<L, R> identityLeft(L left) {
        return new Identity<>(left, (R) null);
    }

    public static <L, R> Choice<L, R> identityRight(R right) {
        return new Identity<>((L) null, right);
    }

    static class Identity<L, R> extends Choice<L, R> {
        protected Identity(L left, R right) {
            super(left, right);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Choice<?, ?> pair = (Choice<?, ?>) o;
            return this.left() == pair.left() && this.right() == pair.right();
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.left()) * 31 + System.identityHashCode(this.right());
        }
    }

    public boolean hasLeft() {
        return this.left() != null;
    }

    public boolean hasRight() {
        return this.right() != null;
    }


}
