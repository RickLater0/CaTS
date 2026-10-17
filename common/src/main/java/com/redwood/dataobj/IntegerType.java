package com.redwood.dataobj;

import com.redwood.interfaces.UserType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Comparator;

public class IntegerType implements Comparable<IntegerType>, UserType, Serializable {

    private static final String TYPE_NAME = "Integer";
    private final int value;

    /// Создание обёртки с конкретным значением
    public IntegerType(int value) {
        this.value = value;
    }

    /// Создание обёртки-прототипа (ноль)
    public IntegerType() {
        this.value = 0;
    }

    public int getValue() {
        return value;
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
    public Comparator getTypeComparator() {
        return Comparator.comparingInt(IntegerType::getValue);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
