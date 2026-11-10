package com.redwood.dataobj;

import com.redwood.interfaces.UserType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Comparator;

public record IntegerType(int value) implements Comparable<IntegerType>, UserType, Serializable {

    private static final String TYPE_NAME = "Integer";

    /// Создание обёртки-прототипа (ноль)
    public IntegerType() {
        this(0);
    }

    public static IntegerType copyOf(IntegerType other) {
        return new IntegerType(other.value);
    }

    @Override
    public int compareTo(IntegerType other) {
        return Integer.compare(this.value, other.value);
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public Object create() {
        return new IntegerType();
    }

    @Override
    public Object readValue(InputStreamReader in) {
        try (BufferedReader br = new BufferedReader(in)) {
            String line = br.readLine();
            if (line == null) return null;
            return parseValue(line);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Object parseValue(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("String can not be empty");
        }
        try {
            return new IntegerType(Integer.parseInt(input.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a valid integer: " + input, e);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Comparator typeComparator() {
        return Comparator.comparingInt(IntegerType::value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
