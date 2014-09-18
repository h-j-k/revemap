package com.ikueb.revemap;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An utilities class to handle {@link Enum}-related {@link Map}s.
 */
public final class EnumMapUtils {

    public static final class DuplicateKeysException extends RuntimeException {

        private static final long serialVersionUID = 6433540910901212502L;

        public DuplicateKeysException() {
            super("Key mapper has produced duplicate keys: ");
        }
    }

    /**
     * Private constructor for utility class.
     */
    private EnumMapUtils() {
        // intentionally blank
    }

    /**
     * Given a {@link Set}<code>&lt;T&gt;</code>, use two {@link Function}s to derive keys of type
     * <code>K</code> and values of type <code>V</code> where <code>K &#8594; V</code>.
     * <p>
     * The merge function used to resolve collisions between values associated with the same key
     * picks the later one.
     *
     * @param <T> the input {@link Set} type.
     * @param <K> the required key type.
     * @param <V> the required value type.
     * @param set the {@link Set} to stream on.
     * @param keyMapper the {@link Function} to use for deriving keys of type <code>K</code> from
     *            the {@link Set}'s elements.
     * @param valueMapper the {@link Function} to use for deriving values of type <code>V</code>
     *            from the {@link Set}'s elements.
     * @param checkDuplicateKeys <code>true</code> if a strict check on duplicate keys is required,
     *            by comparing the resulting {@link Map#size()} with <code>set.size()</code>.
     * @return a {@link Map} with mappings <code>K &#8594; V</code>.
     * @see Collectors#toMap(Function, Function, java.util.function.BinaryOperator)
     */
    private static <T, K, V> Map<K, V> doMap(final Set<T> set, final Function<T, K> keyMapper,
            final Function<T, V> valueMapper, boolean checkDuplicateKeys) {
        final Map<K, V> innerResult = set.stream().collect(
                Collectors.toMap(keyMapper, valueMapper, (earlier, later) -> later));
        if (checkDuplicateKeys && innerResult.size() != set.size()) {
            throw new DuplicateKeysException();
        }
        return innerResult;
    }

    /**
     * Gets the {@link Enum}'s values via reflection. All checked {@link Exception}s are wrapped and
     * thrown as {@link RuntimeException}s.
     *
     * @param <E> the {@link Enum} type.
     * @param forEnum the {@link Enum} to represent.
     * @return a {@link Set} containing the {@link Enum}'s values.
     */
    private static <E extends Enum<E>> Set<E> getEnumValues(final Class<E> forEnum) {
        validateArguments(forEnum);
        try {
            return Stream.of((E[]) forEnum.getDeclaredMethod("values").invoke(forEnum)).collect(
                    Collectors.toSet());
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method exists solely to throw {@link IllegalArgumentException} instead of
     * {@link NullPointerException}, when encountering <code>null</code> arguments.
     *
     * @param args the arguments to check for <code>null</code>.
     */
    private static void validateArguments(final Object... args) {
        if (args == null) {
            throw new IllegalArgumentException(new NullPointerException());
        }
        for (final Object o : args) {
            if (o == null) {
                throw new IllegalArgumentException(new NullPointerException());
            }
        }
    }

    /**
     * Reverses the <code>T &#8594; E</code> mappings of {@link Map}<code>&lt;T, E&gt;</code>
     * <em>while considering</em> the possibility of duplicate <code>E &#8594; T</code> mappings. As
     * such, the values of the resulting {@link Map} are of type {@link Set}<code>&lt;T&gt;</code>.
     * <p>
     * Internally, {@link EnumMap} is the implementation for the resulting {@link Map}, and values
     * for the {@link Set} are accumulated with {@link Collectors#toSet()}.
     *
     * @param <T> the key type of the source {@link Map}.
     * @param <E> the {@link Enum} type.
     * @param forEnum the {@link Enum} to represent.
     * @param map the {@link Map} with mappings <code>T &#8594; E</code>.
     * @return a {@link Map} with mappings <code>E &#8594; Set&lt;T&gt;</code>.
     * @see #convertToSimpleEnumMap(Map)
     * @see Collectors#groupingBy(Function, java.util.stream.Collector)
     * @see Collectors#toSet()
     */
    public static <T, E extends Enum<E>> Map<E, Set<T>> convertToEnumMap(final Class<E> forEnum,
            final Map<T, E> map) {
        validateArguments(forEnum, map);
        final Map<E, Set<T>> result = new EnumMap<>(forEnum);
        result.putAll(map
                .entrySet()
                .stream()
                .collect(
                        Collectors.groupingBy(Entry::getValue,
                                Collectors.mapping(Entry::getKey, Collectors.toSet()))));
        return result;
    }

    /**
     * Reverses the <code>T &#8594; E</code> mappings of {@link Map}<code>&lt;T, E&gt;</code>
     * <em>without considering</em> the possibility of duplicate <code>E &#8594; T</code> mappings.
     * As such, the values of the resulting {@link Map} are of type <code>T</code>, with mappings
     * streamed later over-riding the earlier ones.
     * <p>
     * Internally, {@link EnumMap} is the implementation for the resulting {@link Map}.
     *
     * @param <T> the key type of the source {@link Map}.
     * @param <E> the {@link Enum} type.
     * @param map the {@link Map} with mappings <code>T &#8594; E</code>.
     * @return a {@link Map} with mappings <code>E &#8594; T</code>.
     * @see #convertToEnumMap(Class, Map)
     */
    public static <T, E extends Enum<E>> Map<E, T> convertToSimpleEnumMap(final Map<T, E> map) {
        validateArguments(map);
        return new EnumMap<>(doMap(map.entrySet(), Entry::getValue, Entry::getKey, false));
    }

    /**
     * Creates a {@link Map} with mappings <code>E &#8594; T</code>, where values are derived using
     * a {@link Function}.
     *
     * @param <T> the value type of the resulting {@link Map}.
     * @param <E> the {@link Enum} type.
     * @param forEnum the {@link Enum} to represent.
     * @param enumMapper the {@link Function} to use to derive the values for the resulting
     *            {@link Map}.
     * @return a {@link Map} with mappings <code>E &#8594; T</code>.
     * @see #createEnumMap(Set, Function)
     */
    public static <T, E extends Enum<E>> Map<E, T> createEnumMap(final Class<E> forEnum,
            final Function<E, T> enumMapper) {
        return createEnumMap(getEnumValues(forEnum), enumMapper);
    }

    /**
     * Creates a {@link Map} with mappings <code>E &#8594; T</code>, where values are derived using
     * a {@link Function}.
     * <p>
     * Internally, {@link EnumMap} is the implementation for the resulting {@link Map}.
     *
     * @param <T> the value type of the resulting {@link Map}.
     * @param <E> the {@link Enum} type.
     * @param enumSet the {@link Set} of {@link Enum} to represent.
     * @param enumMapper the {@link Function} to use to derive the values for the resulting
     *            {@link Map}.
     * @return a {@link Map} with mappings <code>E &#8594; T</code>.
     */
    public static <T, E extends Enum<E>> Map<E, T> createEnumMap(final Set<E> enumSet,
            final Function<E, T> enumMapper) {
        validateArguments(enumSet, enumMapper);
        return new EnumMap<>(doMap(enumSet, Function.identity(), enumMapper, false));
    }

    /**
     * Creates a {@link Map} with <code>E</code> as the keys and <code>E</code>'s
     * {@link #toString()} for the values.
     *
     * @param <E> the {@link Enum} type.
     * @param forEnum the {@link Enum} to represent.
     * @return a {@link Map} with mappings <code>E &#8594; String</code>.
     * @see #createEnumMap(Set)
     */
    public static <E extends Enum<E>> Map<E, String> createEnumMap(final Class<E> forEnum) {
        return createEnumMap(getEnumValues(forEnum));
    }

    /**
     * Creates a {@link Map} with <code>E</code> as the keys and <code>E</code>'s
     * {@link #toString()} for the values.
     *
     * @param <E> the {@link Enum} type.
     * @param enumSet the {@link Set} of {@link Enum} to represent.
     * @return a {@link Map} with mappings <code>E &#8594; String</code>.
     * @see #createEnumMap(Class, Function)
     */
    public static <E extends Enum<E>> Map<E, String> createEnumMap(final Set<E> enumSet) {
        return createEnumMap(enumSet, (value) -> value.toString());
    }

    /**
     * Creates a {@link Map} with mappings <code>T &#8594; E</code>, where the keys are derived
     * using a {@link Function}.
     * <p>
     * Internally, {@link HashMap} is the implementation for the resulting {@link Map}.
     *
     * @param <T> the key type of the resulting {@link Map}.
     * @param <E> the {@link Enum} type.
     * @param forEnum the {@link Enum} to represent.
     * @param enumMapper the {@link Function} to use for deriving the {@link Map}'s keys.
     * @return a {@link Map} with mappings <code>T &#8594; E</code>.
     * @see #createReverseEnumMap(Set, Function)
     * @throws DuplicateKeysException if the <code>enumMapper</code> produces duplicate keys.
     */
    public static <T, E extends Enum<E>> Map<T, E> createReverseEnumMap(final Class<E> forEnum,
            final Function<E, T> enumMapper) {
        return createReverseEnumMap(getEnumValues(forEnum), enumMapper);
    }

    /**
     * Creates a {@link Map} with mappings <code>T &#8594; E</code>, where the keys are derived
     * using a {@link Function}.
     * <p>
     * Internally, {@link HashMap} is the implementation for the resulting {@link Map}.
     *
     * @param <T> the key type of the resulting {@link Map}.
     * @param <E> the {@link Enum} type.
     * @param enumSet the {@link Set} of {@link Enum} to represent.
     * @param enumMapper the {@link Function} to use for deriving the {@link Map}'s keys.
     * @return a {@link Map} with mappings <code>T &#8594; E</code>.
     * @see #modifyReverseEnumMap(Set, Function, Map)
     * @throws DuplicateKeysException if the <code>enumMapper</code> produces duplicate keys.
     */
    public static <T, E extends Enum<E>> Map<T, E> createReverseEnumMap(final Set<E> enumSet,
            final Function<E, T> enumMapper) {
        return modifyReverseEnumMap(enumSet, enumMapper, new HashMap<>());
    }

    /**
     * Creates a {@link Map} with <code>E</code>'s {@link #toString()} for the keys and
     * <code>E</code> as the values.
     *
     * @param <E> the {@link Enum} type.
     * @param forEnum the {@link Enum} to represent.
     * @return a {@link Map} with mappings <code>String &#8594; E</code>.
     * @see #createReverseEnumMap(Set)
     * @throws DuplicateKeysException if <code>E</code>'s {@link #toString()} produces duplicate
     *             keys.
     */
    public static <E extends Enum<E>> Map<String, E> createReverseEnumMap(final Class<E> forEnum) {
        return createReverseEnumMap(getEnumValues(forEnum));
    }

    /**
     * Reverses the <code>E &#8594; T</code> mappings of <code>map</code>.
     *
     * @param <T> the key type of the resulting {@link Map}.
     * @param <E> the {@link Enum} type.
     * @param map the {@link Map} to derive the mappings from.
     * @return a {@link Map} with mappings <code>T &#8594; E</code>.
     * @throws DuplicateKeysException if there is more than one <code>E &#8594; T</code> mapping,
     *             producing duplicate keys.
     */
    public static <T, E extends Enum<E>> Map<T, E> createReverseEnumMap(final Map<E, T> map) {
        validateArguments(map);
        return doMap(map.entrySet(), Entry::getValue, Entry::getKey, true);
    }

    /**
     * Creates a {@link Map} with <code>E</code>'s {@link #toString()} for the keys and
     * <code>E</code> as the values.
     *
     * @param <E> the {@link Enum} type.
     * @param enumSet the {@link Set} of {@link Enum} to represent.
     * @return a {@link Map} with mappings <code>String &#8594; E</code>.
     * @see #createReverseEnumMap(Set, Function)
     * @throws DuplicateKeysException if <code>E</code>'s {@link #toString()} produces duplicate
     *             keys.
     */
    public static <E extends Enum<E>> Map<String, E> createReverseEnumMap(final Set<E> enumSet) {
        return createReverseEnumMap(enumSet, (value) -> value.toString());
    }

    /**
     * Modifies a {@link Map} by putting mappings <code>T &#8594; E</code>, where keys are derived
     * using a {@link Function}.
     *
     * @param <T> the key type of the resulting {@link Map}.
     * @param <E> the {@link Enum} type.
     * @param forEnum the {@link Enum} to represent.
     * @param enumMapper the {@link Function} to use for deriving the {@link Map}'s keys.
     * @param result the {@link Map} to put the mappings to.
     * @return the <code>result</code> {@link Map}.
     * @see #modifyReverseEnumMap(Set, Function, Map)
     * @throws DuplicateKeysException if the <code>enumMapper</code> produces duplicate keys.
     */
    public static <T, E extends Enum<E>> Map<T, E> modifyReverseEnumMap(final Class<E> forEnum,
            final Function<E, T> enumMapper, final Map<T, E> result) {
        return modifyReverseEnumMap(getEnumValues(forEnum), enumMapper, result);
    }

    /**
     * Modifies a {@link Map} by putting mappings <code>T &#8594; E</code>, where keys are derived
     * using a {@link Function}.
     *
     * @param <T> the key type of the resulting {@link Map}.
     * @param <E> the {@link Enum} type.
     * @param enumSet the {@link Set} of {@link Enum} to represent.
     * @param enumMapper the {@link Function} to use for deriving the {@link Map}'s keys.
     * @param result the {@link Map} to put the mappings to.
     * @return the <code>result</code> {@link Map}.
     * @throws DuplicateKeysException if the <code>enumMapper</code> produces duplicate keys.
     */
    public static <T, E extends Enum<E>> Map<T, E> modifyReverseEnumMap(final Set<E> enumSet,
            final Function<E, T> enumMapper, final Map<T, E> result) {
        validateArguments(enumSet, enumMapper, result);
        result.putAll(doMap(enumSet, enumMapper, Function.identity(), true));
        return result;
    }

}
