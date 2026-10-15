package dataobj;

import interfaces.UserType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VectorPolar implements Comparable<VectorPolar>, UserType, Serializable {

    private static final String typeName = "Polar vector";
    private static final double TWO_PI = 2 * Math.PI;
    private final double length;
    private final double degree;

    /// Создание вектора
    /// @param length - длина вектора
    /// @param degree - угол вектора в радианах, остаётся только значения до 2Pi
    /// если length - отрицательно, разворачиваем вектор
    public VectorPolar(double length, double degree){
        if(length < 0)
            throw new IllegalArgumentException("Length must be positive double");

        double norm_degree = degree % TWO_PI;
        if(norm_degree < 0)
            norm_degree += TWO_PI;

        this.length = length;
        this.degree = norm_degree;
    }
    /// Создание нуль-вектора
    public VectorPolar() {
        this.length = 0;
        this.degree = 0;
    }

    private static VectorPolar normalize(VectorPolar vec){
        return new VectorPolar(1, vec.degree);
    }

    public double getLength() {
        return length;
    }
    public double getDegree() {
        return degree;
    }

    public static VectorPolar copyOf(VectorPolar vec){
        return new VectorPolar(vec.length, vec.degree);
    }

    @Override
    public int compareTo(VectorPolar other) {
        double myProjectionOnX = this.length * Math.cos(this.degree);
        double otherProjectionOnX = other.getLength() * Math.cos(other.getDegree());

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
        String fullRegex = "\\(\\s*(" + doubleRegex + ")\\s*,\\s*(" + doubleRegex + ")\\s*\\)";

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
    public Comparator getTypeComparator() {
        return Comparator.comparingDouble(VectorPolar::getLength).
                thenComparingDouble(VectorPolar::getDegree);
    }

    @Override
    public String toString(){
        return "(" + this.length + ',' + this.degree + ")";
    }
}
