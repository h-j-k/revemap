# Revemap

[![Build Status](https://travis-ci.org/h-j-k/revemap.svg?branch=master)](https://travis-ci.org/h-j-k/revemap) 
[![codecov.io](http://codecov.io/github/h-j-k/revemap/coverage.svg?branch=master)](http://codecov.io/github/h-j-k/revemap?branch=master)
[![Quality Gate](https://sonarqube.com/api/badges/gate?key=com.ikueb:revemap)](https://sonarqube.com/dashboard/?id=com.ikueb:revemap)
[![Technical Debt Ratio](https://sonarqube.com/api/badges/measure?key=com.ikueb:revemap&metric=sqale_debt_ratio)](https://sonarqube.com/dashboard/?id=com.ikueb:revemap)
[![Comments](https://sonarqube.com/api/badges/measure?key=com.ikueb:revemap&metric=comment_lines_density)](https://sonarqube.com/dashboard/?id=com.ikueb:revemap)

**Rev**erse **E**num **Map**: Java 8-based `EnumMap` utility class.

[Homepage](https://h-j-k.github.io/revemap)

[GitHub project page](https://github.com/h-j-k/revemap)

[Javadocs](https://h-j-k.github.io/revemap/javadoc)

# Motivation

Creating a `Map<String, Enum>` lookup map in the good old days resembles the following:

    public static final Map<String, Enum> generateLookupMap() {
        Map<String, Enum> result = new HashMap<>();
        for (MyEnum value : EnumSet.allOf(MyEnum.class)) {
            result.put(value.getDescription(), value);
        }
        return result;
    }

Java 8's `Stream` simplifies that somewhat:

    public static final Map<String, Enum> LOOKUP = 
        EnumSet.allOf(MyEnum.class).stream().collect(
            Collectors.toMap(MyEnum::getDescription, Function.identity()));

Revemap's `EnumMapUtils` class is doing that under-the-hood to simplify the invocation:

    public static final Map<String, Enum> LOOKUP = 
        EnumMapUtils.createReverseEnumMap(MyEnum.class);

In addition to the simplified example above, Revemap handles the following too:

* Converting a `Map<T, Enum>` map to be keyed by the `enum` values, with implementations to handle duplicate values.
* Create `EnumMap`s with the `enum`s' `toString()` as map values.
* Creating `Map<T, Enum>` maps given a `Function<Enum, T> mapper` for the keys.

# Bugs/feedback

Please make use of the GitHub features to report any bugs, issues, or even pull requests. :)

Enjoy!