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

    private StopOnModifyIterator stoppingIterator() {
        StopOnModifyIterator iterator = new StopOnModifyIterator();
        this.linkedIterators.add(iterator);
        return iterator;
    }

    public Iterator<T> stopOnModifyIterator() {
        return this.stoppingIterator();
    }

    public Stream<T> stopOnModifyStream() {
        StopOnModifyIterator iterator = this.stoppingIterator();
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
        this.linkedIterators.forEach(iter -> iter.notifyDiverge(index, removal));
        this.modCount++;
        this.modify();
    }

    public class FailFastIterator implements Iterator<T> {
        private int traversed = 0;
        private int currIndex = 0;
        private int expectedModCount = modCount;

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

        private void checkMod() {
            if (expectedModCount != modCount) throw new ConcurrentModificationException();
        }
    }

    public class StopOnModifyIterator implements Iterator<T> {
        private int traversed = 0;
        private int currIndex = 0;
        private boolean diverged = false;

        public void notifyDiverge(int index, boolean removal) {
            if ((index < currIndex && removal) ||       //If an element was removed from stuff we already saw, OR
                    (index >= currIndex && !removal) || //an element was added in our future, OR
                    this.traversed >= size) {           //we no longer have room to iterate
                this.diverged = true;
                linkedIterators.remove(this);
            }
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
        return this.has(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new FailFastIterator();
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
        return insert(value, true);
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

        for (int i = index(element, local.length); i < local.length; i++) {
            T curr = (T) local[i];
            if (curr == null) { //We don't have it
                operation.getAsBoolean();
                return;
            } else if (element == curr) { //We do have it
                remove(element); //Remove it for now
                if (operation.getAsBoolean()) {
                    //It did change
                    insert(element, true);
                    this.modify();
                } else {
                    //It didn't change
                    insert(element, false);
                }
                return;
            }
        }

        //We don't have it
        operation.getAsBoolean();
    }

    private boolean has(Object value) {
        Object[] local = this.table;
        for (int i = index(value, local.length); i < local.length; i++) {
            Object curr = local[i];
            if (curr == null) {
                return false;
            } else if (Objects.equals(value, curr)) {
                return true;
            }
        }
        return false;
    }

    private boolean cull(Object value) {
        Object[] local = this.table;

        int index = index(value, local.length);
        int foundAt = -1;

        for (int i = index(value, local.length); i < local.length; i++) {
            Object curr = local[i];
            if (curr == null) {
                return false;
            } else if (Objects.equals(value, curr)) {
                foundAt = i;
                local[i] = null;
                this.size--;
                this.divergeAt(i, true);
                break;
            }
        }

        if (foundAt != -1 && foundAt != index) {
            //Need to shift elements back

            int cutoff = -1;
            for (int i = foundAt + 1; i < local.length; i++) {
                Object curr = local[i];
                if (curr == null || index(curr, local.length) == i) {
                    cutoff = i;
                    break;
                }
            }

            System.arraycopy(table, foundAt + 1, table, foundAt, cutoff - foundAt);
            return true;
        }
        return false;
    }

    private boolean insert(T branch, boolean trackDiverge) {
        boolean success = insert(branch, this.table, index(branch, this.table.length), trackDiverge);
        if (!success) {
            expand(this.table.length * 2);
            return insert(branch, true);
        } else {
            this.size++;
        }
        return success;
    }

    private boolean insert(T branch, Object[] table, int index, boolean trackDiverge) {
        int i = index;
        for (; i < table.length; i++) {
            T curr = (T) table[i];
            if (curr == null) {
                if (trackDiverge) this.divergeAt(i, true);
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
                    boolean success = insert(value, newTable, index(value, newTable.length), false);
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
        return hash % max;
    }

}
