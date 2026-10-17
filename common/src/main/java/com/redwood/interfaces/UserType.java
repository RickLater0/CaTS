package com.redwood.interfaces;

import java.io.InputStreamReader;
import java.util.Comparator;

public interface UserType{
    String typeName();                          // Имя типа
    Object create();                            // Создает объект
    Object readValue(InputStreamReader in);     // Создает и читает объект
    Object parseValue(String input);            // Создает и парсит содержимое из строки

    @SuppressWarnings("rawtypes")
    Comparator getTypeComparator();             // Возвращает компаратор для сравнения
}
