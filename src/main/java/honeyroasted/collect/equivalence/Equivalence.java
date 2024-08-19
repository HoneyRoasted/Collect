package honeyroasted.collect.equivalence;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class Equivalence<T> {

    public boolean equals(Object left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (!type().isInstance(left) || !type().isInstance(right)) return Objects.equals(left, right);
        return doEquals((T) left, (T) right);
    }

    public int hashCode(Object value) {
        if (value == null) return 0;
        if (!type().isInstance(value)) return Objects.hashCode(value);
        return doHashCode((T) value);
    }

    protected abstract boolean doEquals(T left, T right);

    protected abstract int doHashCode(T val);

    protected Class<T> type() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public Wrapper<T> wrap(T value) {
        return new Wrapper<>(value, this);
    }

    public boolean listEquals(List<T> left, List<T> right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (left.size() != right.size()) return false;

        for (int i = 0; i < left.size(); i++) {
            if (!equals(left.get(i), right.get(i))) {
                return false;
            }
        }

        return true;
    }

    public int listHash(List<T> list) {
        int hash = 1;
        for (T value : list) {
            hash = hash * 31 + hashCode(value);
        }
        return hash;
    }

    public boolean setEquals(Set<T> left, Set<T> right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (left.size() != right.size()) return false;

        for (T lval : left) {
            boolean found = false;
            for (T rval : right) {
                if (equals(lval, rval)) {
                    found = true;
                    break;
                }
            }

            if (!found) return false;
        }

        return true;
    }

    public int setHash(Set<T> set) {
        int hash = 0;
        for (T value : set) {
            hash += hashCode(value);
        }
        return hash;
    }

    static class Wrapper<T> {
        private T value;
        private Equivalence<T> equivalence;

        public Wrapper(T value, Equivalence<T> equivalence) {
            this.value = value;
            this.equivalence = equivalence;
        }

        public T value() {
            return this.value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Wrapper wrapper = (Wrapper) o;
            if (!equivalence.type().isInstance(wrapper.value)) return false;
            return equivalence.equals(value, (T) wrapper.value);
        }

        @Override
        public int hashCode() {
            return equivalence.hashCode(this.value);
        }
    }

}
