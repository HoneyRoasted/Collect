package honeyroasted.collect.equivalence;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class Equivalence<T> {

    public static <T> Equivalence<T> instancing(Class<T> type, Equivalence<? extends T>... children) {
        return new Instancing<>(type, Arrays.asList(children));
    }

    public static <T> Equivalence<T> identity() {
        return new Equivalence.Identity<>();
    }

    public final boolean equals(Object left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (!type().isInstance(left) || !type().isInstance(right)) return Objects.equals(left, right);
        return doEquals((T) left, (T) right);
    }

    public final int hashCode(Object value) {
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

    public boolean arrayEquals(T[] left, T[] right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (left.length != right.length) return false;

        for (int i = 0; i < left.length; i++) {
            if (!equals(left[i], right[i])) {
                return false;
            }
        }

        return true;
    }

    public int arrayHash(T[] list) {
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

    static class Identity<T> extends Equivalence<T> {
        @Override
        protected boolean doEquals(T left, T right) {
            return left == right;
        }

        @Override
        protected int doHashCode(T val) {
            return System.identityHashCode(val);
        }
    }

    static class Instancing<T> extends Equivalence<T> {
        private Class<T> type;
        private Collection<Equivalence<? extends T>> children;

        public Instancing(Class<T> parent, Collection<Equivalence<? extends T>> children) {
            this.type = parent;
            this.children = children;
        }

        @Override
        protected boolean doEquals(T left, T right) {
            for (Equivalence<? extends T> child : children) {
                if (child.type().isInstance(left) && child.type().isInstance(right)) {
                    return child.equals(left, right);
                }
            }
            return false;
        }

        @Override
        protected int doHashCode(T val) {
            for (Equivalence<? extends T> child : children) {
                if (child.type().isInstance(val)) {
                    return child.hashCode(val);
                }
            }
            return 0;
        }

        @Override
        protected Class<T> type() {
            return this.type;
        }
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
            return equivalence.equals(value, wrapper.value);
        }

        @Override
        public int hashCode() {
            return equivalence.hashCode(this.value);
        }
    }

}
