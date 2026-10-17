package com.redwood.dataobj;

import com.redwood.interfaces.UserType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Comparator;

public class StringType implements Comparable<StringType>, UserType, Serializable {

    private static final String TYPE_NAME = "String";
    private final String value;

    /// Создание обёртки с конкретным значением
    public StringType(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value can not be null");
        }
        this.value = value;
    }

    /// Создание обёртки-прототипа (пустая строка)
    public StringType() {
        this.value = "";
    }

    public String getValue() {
        return value;
    }

    public static StringType copyOf(StringType other) {
        return new StringType(other.value);
    }

    @Override
    public int compareTo(StringType other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public Object create() {
        return new StringType();
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
        if (input == null) {
            throw new IllegalArgumentException("String can not be null");
        }
        // Для строки содержимое == значение, пустая строка допустима
        return new StringType(input);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Comparator getTypeComparator() {
        return Comparator.comparing(StringType::getValue);
    }

    @Override
    public String toString() {
        return value;
    }
}
