/*
 * Copyright 2015 h-j-k. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ikueb.revemap;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An utilities class to handle {@code enum}-related {@link Map}s.
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
        // empty
    }

    /**
     * Create a {@link Map} by applying key and value mappers to the {@link Set}.
     *
     * @param <T>                the input {@link Set} type.
     * @param <K>                the required key type.
     * @param <V>                the required value type.
     * @param set                the {@link Set} to stream on.
     * @param keyMapper          the {@link Function} to use for mapping keys of type {@code K}
     *                           from the {@link Set}'s elements.
     * @param valueMapper        the {@link Function} to use for mapping values of type
     *                           {@code V} from the {@link Set}'s elements.
     * @param checkDuplicateKeys {@code true} if a strict check on duplicate keys is
     *                           required, by comparing the resulting {@link Map#size()} with
     *                           {@code set.size()}.
     * @return a {@link Map} with mappings {@code K → V}.
     * @implNote the merge function used to resolve collisions between duplicate keys picks the later value encountered
     * @see Collectors#toMap(Function, Function, java.util.function.BinaryOperator)
     */
    private static <T, K, V> Map<K, V> doMap(Set<T> set, Function<T, K> keyMapper,
                                             Function<T, V> valueMapper, boolean checkDuplicateKeys) {
        return Optional.of(set.stream().collect(Collectors.toMap(keyMapper, valueMapper, (a, b) -> b)))
                .filter(m -> !checkDuplicateKeys || m.size() == set.size())
                .orElseThrow(DuplicateKeysException::new);
    }

    /**
     * This method exists solely to throw {@link IllegalArgumentException} instead of
     * {@link NullPointerException}, when encountering {@code null} arguments.
     *
     * @param args the arguments to check for {@code null}.
     */
    private static void validateArguments(Object... args) {
        if (Objects.isNull(args) || !Stream.of(args).allMatch(Objects::nonNull)) {
            throw new IllegalArgumentException(new NullPointerException());
        }
    }

    /**
     * Reverses the {@code T → E} mappings of {@link Map}{@code <T, E>}
     * <em>while considering</em> the possibility of duplicate {@code E → T} mappings.
     * As such, the values of the resulting {@link Map} are of type {@link Set}
     * {@code <T>}.
     *
     * @param <T>     the key type of the source {@link Map}.
     * @param <E>     the {@code enum} type.
     * @param forEnum the {@code enum} to represent.
     * @param map     the {@link Map} with mappings {@code T → E}.
     * @return a {@link Map} with mappings {@code E → Set<T>}.
     * @see #convertToSimpleEnumMap(Map)
     * @see Collectors#groupingBy(Function, java.util.stream.Collector)
     * @see Collectors#toSet()
     */
    public static <T, E extends Enum<E>> Map<E, Set<T>> convertToEnumMap(Class<E> forEnum,
                                                                         Map<T, E> map) {
        validateArguments(forEnum, map);
        return map.entrySet().stream().collect(Collectors.groupingBy(Entry::getValue,
                () -> new EnumMap<>(forEnum),
                Collectors.mapping(Entry::getKey, Collectors.toSet())));
    }

    /**
     * Reverses the {@code T → E} mappings of {@link Map}{@code <T, E>}
     * <em>without considering</em> the possibility of duplicate {@code E → T} mappings.
     * As such, the values of the resulting {@link Map} are of type {@code T}, with
     * mappings streamed later overriding the earlier ones.
     *
     * @param <T> the key type of the source {@link Map}.
     * @param <E> the {@code enum} type.
     * @param map the {@link Map} with mappings {@code T → E}.
     * @return a {@link Map} with mappings {@code E → T}.
     * @see #convertToEnumMap(Class, Map)
     */
    public static <T, E extends Enum<E>> Map<E, T> convertToSimpleEnumMap(Map<T, E> map) {
        validateArguments(map);
        return new EnumMap<E, T>(doMap(map.entrySet(), Entry::getValue, Entry::getKey, false));
    }

    /**
     * Creates a {@link Map} with mappings {@code E → T}, where values are derived using
     * a {@link Function}.
     *
     * @param <T>        the value type of the resulting {@link Map}.
     * @param <E>        the {@code enum} type.
     * @param forEnum    the {@code enum} to represent.
     * @param enumMapper the {@link Function} to use to derive the values for the
     *                   resulting {@link Map}.
     * @return a {@link Map} with mappings {@code E → T}.
     * @see #createEnumMap(Set, Function)
     */
    public static <T, E extends Enum<E>> Map<E, T> createEnumMap(Class<E> forEnum,
                                                                 Function<E, T> enumMapper) {
        return createEnumMap(EnumSet.allOf(forEnum), enumMapper);
    }

    /**
     * Creates a {@link Map} with mappings {@code E → T}, where values are derived using
     * a {@link Function}.
     *
     * @param <T>        the value type of the resulting {@link Map}.
     * @param <E>        the {@code enum} type.
     * @param enumSet    the {@link Set} of {@code enum} to represent.
     * @param enumMapper the {@link Function} to use to derive the values for the
     *                   resulting {@link Map}.
     * @return a {@link Map} with mappings {@code E → T}.
     */
    public static <T, E extends Enum<E>> Map<E, T> createEnumMap(Set<E> enumSet,
                                                                 Function<E, T> enumMapper) {
        validateArguments(enumSet, enumMapper);
        return new EnumMap<>(doMap(enumSet, Function.identity(), enumMapper, false));
    }

    /**
     * Creates a {@link Map} of {@code enums} as keys and their {@link Enum#toString()}
     * representation as values.
     *
     * @param <E>     the {@code enum} type.
     * @param forEnum the {@code enum} to represent.
     * @return a {@link Map} with mappings {@code E → String}.
     * @see #createEnumMap(Set)
     */
    public static <E extends Enum<E>> Map<E, String> createEnumMap(Class<E> forEnum) {
        return createEnumMap(EnumSet.allOf(forEnum));
    }

    /**
     * Creates a {@link Map} of {@code enums} as keys and their {@link Enum#toString()}
     * representation as values.
     *
     * @param <E>     the {@code enum} type.
     * @param enumSet the {@link Set} of {@code enum} to represent.
     * @return a {@link Map} with mappings {@code E → String}.
     * @see #createEnumMap(Class, Function)
     */
    public static <E extends Enum<E>> Map<E, String> createEnumMap(Set<E> enumSet) {
        return createEnumMap(enumSet, Enum::toString);
    }

    /**
     * Creates a {@link Map} with mappings {@code T → E}, where the keys are mapped
     * using a {@link Function}.
     *
     * @param <T>        the key type of the resulting {@link Map}.
     * @param <E>        the {@code enum} type.
     * @param forEnum    the {@code enum} to represent.
     * @param enumMapper the {@link Function} to use for mapping the {@link Map}'s keys.
     * @return a {@link Map} with mappings {@code T → E}.
     * @throws DuplicateKeysException if the {@code enumMapper} produces duplicate keys.
     * @see #createReverseEnumMap(Set, Function)
     */
    public static <T, E extends Enum<E>> Map<T, E> createReverseEnumMap(Class<E> forEnum,
                                                                        Function<E, T> enumMapper) {
        return createReverseEnumMap(EnumSet.allOf(forEnum), enumMapper);
    }

    /**
     * Creates a {@link Map} with mappings {@code T → E}, where the keys are mapped
     * using a {@link Function}.
     *
     * @param <T>        the key type of the resulting {@link Map}.
     * @param <E>        the {@code enum} type.
     * @param enumSet    the {@link Set} of {@code enum} to represent.
     * @param enumMapper the {@link Function} to use for mapping the {@link Map}'s keys.
     * @return a {@link Map} with mappings {@code T → E}.
     * @throws DuplicateKeysException if the {@code enumMapper} produces duplicate keys.
     * @see #modifyReverseEnumMap(Set, Function, Map)
     */
    public static <T, E extends Enum<E>> Map<T, E> createReverseEnumMap(Set<E> enumSet,
                                                                        Function<E, T> enumMapper) {
        return modifyReverseEnumMap(enumSet, enumMapper, new HashMap<>());
    }

    /**
     * Creates a {@link Map} of {@code enums} as values and their
     * {@link Enum#toString()} representation as keys.
     *
     * @param <E>     the {@code enum} type.
     * @param forEnum the {@code enum} to represent.
     * @return a {@link Map} with mappings {@code String → E}.
     * @throws DuplicateKeysException if {@code E}'s {@link #toString()} produces
     *                                duplicate keys.
     * @see #createReverseEnumMap(Set)
     */
    public static <E extends Enum<E>> Map<String, E> createReverseEnumMap(Class<E> forEnum) {
        return createReverseEnumMap(EnumSet.allOf(forEnum));
    }

    /**
     * Reverses the {@code E → T} mappings of {@code map}.
     *
     * @param <T> the key type of the resulting {@link Map}.
     * @param <E> the {@code enum} type.
     * @param map the {@link Map} to derive the mappings from.
     * @return a {@link Map} with mappings {@code T → E}.
     * @throws DuplicateKeysException if there is more than one {@code E → T} mapping,
     *                                producing duplicate keys.
     */
    public static <T, E extends Enum<E>> Map<T, E> createReverseEnumMap(Map<E, T> map) {
        validateArguments(map);
        return doMap(map.entrySet(), Entry::getValue, Entry::getKey, true);
    }

    /**
     * Creates a {@link Map} of {@code enums} as values and their
     * {@link Enum#toString()} representation as keys.
     *
     * @param <E>     the {@code enum} type.
     * @param enumSet the {@link Set} of {@code enum} to represent.
     * @return a {@link Map} with mappings {@code String → E}.
     * @throws DuplicateKeysException if {@code E}'s {@link #toString()} produces
     *                                duplicate keys.
     * @see #createReverseEnumMap(Set, Function)
     */
    public static <E extends Enum<E>> Map<String, E> createReverseEnumMap(Set<E> enumSet) {
        return createReverseEnumMap(enumSet, Enum::toString);
    }

    /**
     * Modifies a {@link Map} by putting mappings {@code T → E}, where keys are derived
     * using a {@link Function}.
     *
     * @param <T>        the key type of the resulting {@link Map}.
     * @param <E>        the {@code enum} type.
     * @param forEnum    the {@code enum} to represent.
     * @param enumMapper the {@link Function} to use for mapping the {@link Map}'s keys.
     * @param result     the {@link Map} to put the mappings to.
     * @return the {@code result} {@link Map}.
     * @throws DuplicateKeysException if the {@code enumMapper} produces duplicate keys.
     * @see #modifyReverseEnumMap(Set, Function, Map)
     */
    public static <T, E extends Enum<E>> Map<T, E> modifyReverseEnumMap(Class<E> forEnum,
                                                                        Function<E, T> enumMapper, Map<T, E> result) {
        return modifyReverseEnumMap(EnumSet.allOf(forEnum), enumMapper, result);
    }

    /**
     * Modifies a {@link Map} by putting mappings {@code T → E}, where keys are derived
     * using a {@link Function}.
     *
     * @param <T>        the key type of the resulting {@link Map}.
     * @param <E>        the {@code enum} type.
     * @param enumSet    the {@link Set} of {@code enum} to put.
     * @param enumMapper the {@link Function} to use for mapping the {@link Map}'s keys.
     * @param result     the {@link Map} to put the mappings to.
     * @return the {@code result} {@link Map}.
     * @throws DuplicateKeysException if the {@code enumMapper} produces duplicate keys.
     */
    public static <T, E extends Enum<E>> Map<T, E> modifyReverseEnumMap(Set<E> enumSet,
                                                                        Function<E, T> enumMapper, Map<T, E> result) {
        validateArguments(enumSet, enumMapper, result);
        result.putAll(doMap(enumSet, enumMapper, Function.identity(), true));
        return result;
    }
}
