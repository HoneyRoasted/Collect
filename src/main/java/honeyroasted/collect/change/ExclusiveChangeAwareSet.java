package honeyroasted.collect.change;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ExclusiveChangeAwareSet<T extends ChangingMergingElement<T>> implements Set<T> {
    private Object[] table;
    private int size = 0;

    private int modCount = 0;

    private List<StopOnModifyIterator> linkedIterators = new ArrayList<>();
    private List<Runnable> modifyListeners = new ArrayList<>();

    public ExclusiveChangeAwareSet(int capacity) {
        this.table = new Object[capacity];
    }

    public ExclusiveChangeAwareSet() {
        this(256);
    }

    public Iterator<T> ignoreModifyIterator() {
        return new FailFastIterator(false);
    }

    public Stream<T> ignoreModifyStream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(ignoreModifyIterator(), 0), false);
    }


    public StopOnModifyIterator stopOnModifyIterator() {
        StopOnModifyIterator iterator = new StopOnModifyIterator();
        this.linkedIterators.add(iterator);
        return iterator;
    }

    public Stream<T> stopOnModifyStream() {
        StopOnModifyIterator iterator = this.stopOnModifyIterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL & Spliterator.DISTINCT), false)
                .onClose(() -> linkedIterators.remove(iterator));
    }

    public Set<T> setCopy() {
        return this.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void addListener(Runnable modifyListener) {
        this.modifyListeners.add(modifyListener);
    }

    public void removeListener(Runnable modifyListener) {
        this.modifyListeners.remove(modifyListener);
    }

    private void modify() {
        this.modifyListeners.forEach(Runnable::run);
    }

    private void divergeAt(int index, boolean removal) {
        Iterator<StopOnModifyIterator> iterIter = this.linkedIterators.iterator();
        while (iterIter.hasNext()) {
            StopOnModifyIterator iter = iterIter.next();
            boolean shouldDrop = iter.notifyDiverge(index, removal);
            if (shouldDrop) iterIter.remove();
        }
        this.modCount++;
        this.modify();
    }

    public class FailFastIterator implements Iterator<T> {
        private int traversed = 0;
        private int currIndex = 0;
        private int expectedModCount;

        public FailFastIterator(boolean checkMods) {
            if (checkMods) {
                this.expectedModCount = modCount;
            } else {
                this.expectedModCount = -1;
            }
        }

        @Override
        public boolean hasNext() {
            checkMod();
            return traversed < size;
        }

        @Override
        public T next() {
            checkMod();

            traversed++;
            Object[] local = table;
            for (int i = currIndex; i < local.length; i++) {
                Object val = local[i];
                if (val != null) {
                    currIndex = i + 1;
                    return (T) val;
                }
            }
            return null;
        }

        @Override
        public void remove() {
            this.currIndex--;
            table[currIndex] = null;
            shift(currIndex);
            divergeAt(currIndex, true);
        }

        private void checkMod() {
            if (expectedModCount != -1 && expectedModCount != modCount) throw new ConcurrentModificationException();
        }
    }

    public class StopOnModifyIterator implements Iterator<T> {
        private int traversed = 0;
        private int currIndex = 0;
        private boolean diverged = false;

        public boolean notifyDiverge(int index, boolean removal) {
            if ((index <= currIndex && removal) ||       //If an element was removed from stuff we already saw, OR
                    (index > currIndex && !removal) || //an element was added in our future, OR
                    this.traversed >= size) {           //we no longer have room to iterate
                this.diverged = true;
                return true;
            }
            return false;
        }

        public boolean isDiverged() {
            return this.diverged;
        }

        @Override
        public boolean hasNext() {
            return !diverged && traversed < size;
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();

            traversed++;
            Object[] local = table;
            for (int i = currIndex; i < local.length; i++) {
                Object val = local[i];
                if (val != null) {
                    currIndex = i + 1;
                    return (T) val;
                }
            }
            return null;
        }

        @Override
        public void remove() {
            this.currIndex--;
            table[currIndex] = null;
            shift(currIndex);
            divergeAt(currIndex, true);
        }
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return this.get(o) != null;
    }

    @Override
    public Iterator<T> iterator() {
        return new FailFastIterator(true);
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(this.iterator(), this.size, Spliterator.NONNULL & Spliterator.DISTINCT);
    }

    @Override
    public Object[] toArray() {
        return this.stream().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return this.stream().toArray(n -> a);
    }

    public boolean add(T value) {
        return insert(value, true, true);
    }

    @Override
    public boolean remove(Object o) {
        return cull(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean flag = false;
        for (T o : c) {
            if (add(o)) flag = true;
        }
        return flag;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Set<Object> toRemove = new HashSet<>();
        for (T child : this) {
            if (!c.contains(child)) {
                toRemove.add(child);
            }
        }
        return removeAll(toRemove);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean flag = false;
        for (Object o : c) {
            if (remove(o)) flag = true;
        }
        return flag;
    }

    @Override
    public void clear() {
        this.divergeAt(-1, true);
        this.size = 0;
        this.table = new Object[this.table.length];
    }

    public void doChange(T element, BooleanSupplier operation) {
        Object[] local = this.table;
        int index = index(element, local.length);

        for (int i = index; i < local.length; i++) {
            T curr = (T) local[i];
            if (curr == null) { //We don't have it
                operation.getAsBoolean();
                return;
            } else if (element == curr) { //We do have it
                if (operation.getAsBoolean()) {
                    //It did change
                    local[i] = null;
                    shift(i);
                    insert(element, true, false);
                    this.modify();
                }
                return;
            }
        }

        //We don't have it
        operation.getAsBoolean();
    }

    public <T> T get(T value) {
        Object[] local = this.table;
        for (int i = index(value, local.length); i < local.length; i++) {
            Object curr = local[i];
            if (curr == null) {
                return null;
            } else if (Objects.equals(value, curr)) {
                return (T) curr;
            }
        }
        return null;
    }

    private boolean shift(int index) {
        Object[] local = this.table;
        if (index != -1) {
            //Need to shift elements back

            int cutoff = -1;
            for (int i = index + 1; i < local.length; i++) {
                Object curr = local[i];
                if (curr == null || index(curr, local.length) == i) {
                    cutoff = i;
                    break;
                }
            }

            if (cutoff != -1) {
                System.arraycopy(table, index + 1, table, index, cutoff - index);
                return true;
            }
        }
        return false;
    }

    private boolean cull(Object value) {
        Object[] local = this.table;

        for (int i = index(value, local.length); i < local.length; i++) {
            Object curr = local[i];
            if (curr == null) {
                return false;
            } else if (Objects.equals(value, curr)) {
                local[i] = null;
                this.size--;
                this.divergeAt(i, true);
                shift(i);
                return true;
            }
        }
        return false;
    }

    private boolean insert(T branch, boolean trackDiverge, boolean trackSize) {
        boolean success = insert(branch, this.table, index(branch, this.table.length), trackDiverge, trackSize);
        if (!success) {
            expand(this.table.length * 2);
            return insert(branch, trackDiverge, trackSize);
        }
        return success;
    }

    private boolean insert(T branch, Object[] table, int index, boolean trackDiverge, boolean trackSize) {
        int i = index;
        for (; i < table.length; i++) {
            T curr = (T) table[i];
            if (curr == null) {
                if (trackDiverge) this.divergeAt(i, true);
                if (trackSize) size++;
                table[i] = branch;
                return true;
            } else if (Objects.equals(branch, curr)) {
                curr.merge(branch);
                return true;
            }
        }
        return false;
    }

    private void expand(int size) {
        if (this.table.length < size) {
            Object[] newTable = new Object[size];
            for (int i = 0; i < this.table.length; i++) {
                T value = (T) this.table[i];
                if (value != null) {
                    boolean success = insert(value, newTable, index(value, newTable.length), false, false);
                    if (!success) {
                        expand(size * 2);
                        return;
                    }
                }
            }
            this.table = newTable;
        }
    }

    private static int index(Object object, int max) {
        return index(Objects.hashCode(object), max);
    }

    private static int index(int hash, int max) {
        return (hash >= 0 ? hash : -hash) % max;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Set<?> that)) return false;
        return containsAll(that) && this.size == that.size();
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (T val : this) {
            result += Objects.hashCode(val);
        }
        return result;
    }
}
