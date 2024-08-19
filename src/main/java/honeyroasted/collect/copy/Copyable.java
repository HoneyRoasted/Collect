package honeyroasted.collect.copy;

public interface Copyable<K, C> {

    default <T extends K> T copy() {
        return copy(null);
    }

    <T extends K> T copy(C context);

}
