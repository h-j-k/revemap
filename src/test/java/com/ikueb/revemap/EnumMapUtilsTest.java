package com.ikueb.revemap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit testing for {@link EnumMapUtils}.
 */
public class EnumMapUtilsTest {

    static final Logger log = LoggerFactory.getLogger(EnumMapUtilsTest.class);
    static final Set<Alphabet> ALL = newSet(Alphabet.values());
    static final Set<Alphabet> RANGE = EnumSet.range(Alphabet.ALFA, Alphabet.CHARLIE);
    static final Function<Alphabet, Integer> GET_ASCII = Alphabet::getAsciiValue;
    static final Function<Alphabet, String> TO_STRING = Alphabet::toString;
    static final Function<Alphabet, Object> LAST_FUNCTION = Alphabet::toString;
    static final Map<Alphabet, Integer> ENUM_TO_INT = mapValues(ALL, GET_ASCII);
    static final Map<Alphabet, Integer> ENUM_TO_INT_RANGE = mapValues(RANGE, GET_ASCII);
    static final Map<Alphabet, String> ENUM_TO_STRING = mapValues(ALL, TO_STRING);
    static final Map<Alphabet, String> ENUM_TO_STRING_RANGE = mapValues(RANGE, TO_STRING);
    static final Map<Integer, Alphabet> INT_TO_ENUM = mapKeys(ALL, GET_ASCII);
    static final Map<Integer, Alphabet> INT_TO_ENUM_RANGE = mapKeys(RANGE, GET_ASCII);
    static final Map<String, Alphabet> STRING_TO_ENUM = mapKeys(ALL, TO_STRING);
    static final Map<String, Alphabet> STRING_TO_ENUM_RANGE = mapKeys(RANGE, TO_STRING);
    static final Map<Object, Alphabet> SOURCE = objectToEnumMap(new HashMap<>(),
            Alphabet::getAsciiValue, LAST_FUNCTION);
    static final Map<Alphabet, Set<Object>> EXPECTED = mapValues(ALL,
            (value) -> newSet(value.getAsciiValue(), value.toString()));
    static final Map<Alphabet, Object> EXPECTED_SIMPLE = mapValues(ALL, LAST_FUNCTION);

    static enum Alphabet {
        ALFA, BRAVO, CHARLIE;

        int getAsciiValue() {
            return ordinal() + 65;
        }

        @Override
        public String toString() {
            return ((char) getAsciiValue()) + super.toString().substring(1).toLowerCase();
        }
    }

    static enum TestCase {
        CONVERT_TO_ENUM_MAP(EnumMapUtils.convertToEnumMap(Alphabet.class, SOURCE), EXPECTED),
        CONVERT_TO_SIMPLE_ENUM_MAP(EnumMapUtils.convertToSimpleEnumMap(SOURCE), EXPECTED_SIMPLE),
        MAP_ENUM_TO_INTEGER(EnumMapUtils.createEnumMap(Alphabet.class, GET_ASCII), ENUM_TO_INT),
        MAP_RANGE_ENUM_TO_INTEGER(EnumMapUtils.createEnumMap(RANGE, GET_ASCII), ENUM_TO_INT_RANGE),
        MAP_ENUM_TO_STRING(EnumMapUtils.createEnumMap(Alphabet.class), ENUM_TO_STRING),
        MAP_RANGE_ENUM_TO_STRING(EnumMapUtils.createEnumMap(RANGE), ENUM_TO_STRING_RANGE),
        MAP_INTEGER_TO_ENUM(EnumMapUtils.createReverseEnumMap(Alphabet.class, GET_ASCII), INT_TO_ENUM),
        MAP_RANGE_INTEGER_TO_ENUM(EnumMapUtils.createReverseEnumMap(RANGE, GET_ASCII), INT_TO_ENUM_RANGE),
        MAP_STRING_TO_ENUM(EnumMapUtils.createReverseEnumMap(Alphabet.class), STRING_TO_ENUM),
        MAP_RANGE_STRING_TO_ENUM(EnumMapUtils.createReverseEnumMap(RANGE), STRING_TO_ENUM_RANGE),
        REVERSE_ENUM_MAP(EnumMapUtils.createReverseEnumMap(ENUM_TO_STRING), STRING_TO_ENUM),
        MODIFY_REVERSE_MAP(EnumMapUtils.modifyReverseEnumMap(Alphabet.class, GET_ASCII,
                new TreeMap<>()), new TreeMap<>(INT_TO_ENUM)),
        MODIFY_RANGE_REVERSE_MAP(EnumMapUtils.modifyReverseEnumMap(RANGE, GET_ASCII,
                newDescendingTreeMap(null)), newDescendingTreeMap(mapKeys(RANGE, GET_ASCII)));

        final Map<?, ?> result;
        final Map<?, ?> expected;

        <K, V> TestCase(final Map<K, V> result, Map<K, V> expected) {
            this.result = result;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return super.toString().replace('_', ' ').toLowerCase();
        }

        void verify() {
            assertThat(result, equalTo(expected));
            log.debug("Results for testing {}:", toString());
            result.forEach((key, value) -> log.debug("Key [{}] => Value [{}]", key, value));
        }
    }

    /**
     * Creates a {@link Map} by deriving keys from a {@link Set} of {@link Alphabet} enums as
     * values.
     *
     * @param set the {@link Set} of {@link Alphabet} {@link Enum}s to use as values.
     * @param keyMapper the {@link Function} to use for deriving keys per {@link Enum} value.
     * @return a {@link Map} with mappings <code>K &#8594; Alphabet</code>.
     */
    private static <K> Map<K, Alphabet> mapKeys(final Set<Alphabet> set,
            final Function<Alphabet, K> keyMapper) {
        return set.stream().collect(Collectors.toMap(keyMapper, Function.identity()));
    }

    /**
     * Creates a {@link Map} by deriving values from a {@link Set} of {@link Alphabet} enums as
     * keys.
     *
     * @param set the {@link Set} of {@link Alphabet} {@link Enum}s to use as keys.
     * @param valueMapper the {@link Function} to use for deriving values per {@link Enum} key.
     * @return a {@link Map} with mappings <code>Alphabet &#8594; V</code>.
     */
    private static <V> Map<Alphabet, V> mapValues(final Set<Alphabet> set,
            final Function<Alphabet, V> valueMapper) {
        return new EnumMap<>(set.stream().collect(
                Collectors.toMap(Function.identity(), valueMapper)));
    }

    /**
     * Wrapper method for creating a {@link Set} from an array.
     *
     * @param values the values to create a {@link Set} for.
     * @return a {@link Set} containing <code>values</code>.
     */
    private static <T> Set<T> newSet(final T... values) {
        return Stream.of(values).collect(Collectors.toSet());
    }

    /**
     * Iteratively calls {@link EnumMapUtils#modifyReverseEnumMap(Class, Function, Map)} with each
     * element of <code>enumMappers</code>.
     *
     * @param result the {@link Map} to use in
     *            {@link EnumMapUtils#modifyReverseEnumMap(Class, Function, Map)}.
     * @param enumMappers the {@link Function}s to use for
     *            {@link EnumMapUtils#modifyReverseEnumMap(Class, Function, Map)}.
     * @return the <code>result</code> {@link Map}.
     */
    private static Map<Object, Alphabet> objectToEnumMap(final Map<Object, Alphabet> result,
            final Function<Alphabet, Object>... enumMappers) {
        for (final Function<Alphabet, Object> current : enumMappers) {
            EnumMapUtils.modifyReverseEnumMap(Alphabet.class, current, result);
        }
        return result;
    }

    /**
     * Creates a new {@link TreeMap} that will sort the keys by descending order instead of the
     * default ascending order.
     *
     * @param map passed to {@link Map#putAll(Map)} if not null.
     * @return a new {@link TreeMap}.
     */
    private static Map<Integer, Alphabet> newDescendingTreeMap(final Map<Integer, Alphabet> map) {
        final Map<Integer, Alphabet> expected = new TreeMap<>((first, second) -> second - first);
        if (map != null) {
            expected.putAll(map);
        }
        return expected;
    }

    @DataProvider(name = "test-cases")
    public Iterator<Object[]> getTestCases() {
        return Stream.of(TestCase.values()).map((current) -> new Object[] { current }).iterator();
    }

    @Test(dataProvider = "test-cases")
    public void testCase(final TestCase current) {
        current.verify();
    }

    @Test(expectedExceptions = EnumMapUtils.DuplicateKeysException.class)
    public void testBadKeyMapper() {
        EnumMapUtils.createReverseEnumMap(Alphabet.class, (value) -> 0);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullArgument() throws Throwable {
        try {
            EnumMapUtils.convertToSimpleEnumMap(null);
        } catch (IllegalArgumentException e) {
            throw e.getCause();
        }
    }

}
