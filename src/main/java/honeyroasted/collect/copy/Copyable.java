package honeyroasted.collect.copy;

import java.lang.reflect.ParameterizedType;

public interface Copyable<K, C> {

    default <T extends K> T copy() {
        return copy(null);
    }

    <T extends K> T copy(C context);


    default Class<K> resultType() {
        return (Class<K>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    default Class<C> contextType() {
        return (Class<C>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }
}
