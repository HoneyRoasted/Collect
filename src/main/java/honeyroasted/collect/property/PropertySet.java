package honeyroasted.collect.property;

import honeyroasted.collect.copy.Copyable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertySet implements Copyable<PropertySet, Object[]> {
    private Set<Object> properties = new HashSet<>();

    public PropertySet attach(Object o) {
        Objects.requireNonNull(o);
        this.properties.add(o);
        return this;
    }

    public PropertySet remove(Object o) {
        this.properties.remove(o);
        return this;
    }

    public PropertySet remove(Class<?> type) {
        this.properties.removeIf(type::isInstance);
        return this;
    }

    public <T> Set<T> all(Class<T> type) {
        return (Set<T>) this.properties.stream().filter(type::isInstance).collect(Collectors.toSet());
    }

    public <T> Optional<T> first(Class<T> type) {
        return (Optional<T>) this.properties.stream().filter(type::isInstance).findFirst();
    }

    public <T> T firstOr(Class<T> type, T failback) {
        return first(type).orElse(failback);
    }

    public boolean has(Class<?> type) {
        return this.properties.stream().anyMatch(type::isInstance);
    }

    public long count(Class<?> type) {
        return this.properties.stream().filter(type::isInstance).count();
    }

    public PropertySet copyFrom(PropertySet other) {
        this.properties.clear();
        this.properties.addAll(other.properties);
        return this;
    }

    public PropertySet inheritFrom(PropertySet other) {
        this.properties.addAll(other.properties);
        return this;
    }

    public PropertySet inheritUnique(PropertySet other) {
        other.properties.stream().filter(obj -> this.properties.stream().noneMatch(curr -> curr.getClass().isInstance(obj)))
                .forEach(this.properties::add);
        return this;
    }

    @Override
    public PropertySet copy(Object... contexts) {
        PropertySet copy = new PropertySet();
        this.properties.forEach(obj -> {
            if (obj instanceof Copyable cp) {
                Object context = null;
                for (Object cand : contexts) {
                    if (cp.isContextType(cand)) {
                        context = cand;
                        break;
                    }
                }

                if (context != null || cp.isContextType(context)) {
                    copy.attach(cp.copy(context));
                } else {
                    copy.attach(obj);
                }
            } else {
                copy.attach(obj);
            }
        });
        return copy;
    }
}
