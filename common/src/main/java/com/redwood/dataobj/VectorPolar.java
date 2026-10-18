package com.redwood.dataobj;

import com.redwood.interfaces.UserType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record VectorPolar(double length, double degree) implements Comparable<VectorPolar>, UserType, Serializable {

    private static final String typeName = "Polar vector";
    private static final double TWO_PI = 2 * Math.PI;

    /// Создание вектора
    ///
    /// @param length - длина вектора
    /// @param degree - угол вектора в радианах, остаётся только значения до 2Pi
    ///
    public VectorPolar(double length, double degree) {
        if (length < 0)
            throw new IllegalArgumentException("Length must be positive double");

        double norm_degree = degree % TWO_PI;
        if (norm_degree < 0)
            norm_degree += TWO_PI;

        this.length = length;
        this.degree = norm_degree;
    }

    /// Создание нуль-вектора
    public VectorPolar() {
        this(0, 0);
    }

    public static VectorPolar normalize(VectorPolar vec) {
        return new VectorPolar(1, vec.degree);
    }

    public static VectorPolar copyOf(VectorPolar vec) {
        return new VectorPolar(vec.length, vec.degree);
    }

    @Override
    public int compareTo(VectorPolar other) {
        double myProjectionOnX = this.length * Math.cos(this.degree);
        double otherProjectionOnX = other.length() * Math.cos(other.degree());

        return Double.compare(myProjectionOnX, otherProjectionOnX);
    }

    @Override
    public String typeName() {
        return typeName;
    }

    @Override
    public VectorPolar create() {
        return new VectorPolar();
    }

    @Override
    public VectorPolar readValue(InputStreamReader in) {
        try {
            String ss = in.readAllAsString();
            return parseValue(ss);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public VectorPolar parseValue(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("String can not be empty");
        }
        String doubleRegex = "[-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?";
        String fullRegex = "\\(\\s*(" + doubleRegex + ")\\s*;\\s*(" + doubleRegex + ")\\s*\\)";

        Pattern pattern = Pattern.compile(fullRegex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            double length = Double.parseDouble(matcher.group(1));
            double degree = Double.parseDouble(matcher.group(2));

            return new VectorPolar(length, degree);
        } else {
            throw new IllegalArgumentException("Incorrect format");
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Comparator typeComparator() {
        return Comparator.comparingDouble(VectorPolar::length).
                thenComparingDouble(VectorPolar::degree);
    }

    @Override
    public String toString() {
        return String.format("(%03f;%3f)", this.length, this.degree);
    }
}
