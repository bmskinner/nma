/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This utility class handles conversions of arrays from primitives to Objects.
 * 
 * @author bms41
 *
 */
public class ArrayConverter {

    List<Object> data = new ArrayList<Object>();

    /**
     * Create with an int array.
     * @param d the int array
     */
    public ArrayConverter(int[] d) {

        for (Object o : d) {
            data.add(o);
        }

    }

    /**
     * Create with a float array.
     * @param d the float array
     */
    public ArrayConverter(float[] d) {

        for (float o : d) {
            data.add(o);
        }

    }

    /**
     * Create with an Integer array.
     * @param d the array
     */
    public ArrayConverter(Integer[] d) {

        for (Object o : d) {
            data.add(o);
        }
    }

    /**
     * Create with a Double array.
     * @param d the array
     */
    public ArrayConverter(Double[] d) {

        for (Object o : d) {
            data.add(o);
        }
    }

    /**
     * Create with a double array.
     * @param d the array
     */
    public ArrayConverter(double[] d) {

        for (Object o : d) {
            data.add(o);
        }
    }

    /**
     * Create with an arbitrary collection.
     * @param d the collection
     */
    public ArrayConverter(Collection<? extends Object> d) {

        for (Object o : d) {
            data.add(o);
        }

    }

    /**
     * Convert the given data to a Double[].
     * 
     * @return a Double array
     * @throws ArrayConversionException
     *             if the data are not Numbers
     */
    public Double[] toDoubleObjectArray() throws ArrayConversionException {

        if (data.isEmpty()) {
            return new Double[0];
            // throw new ArrayConversionException("Cannot convert empty list");
        }

        Object first = data.get(0);

        if (first instanceof Number) {

            Double[] result = new Double[data.size()];

            for (int i = 0; i < data.size(); i++) {
                result[i] = ((Number) data.get(i)).doubleValue();
            }
            return result;

        } else {
            throw new ArrayConversionException("Data is not a number");
        }

    }

    /**
     * Convert the given data to a double[].
     * 
     * @return a double array
     * @throws ArrayConversionException
     *             if the data are not Numbers
     */
    public double[] toDoubleArray() throws ArrayConversionException {

        if (data.isEmpty()) {
            return new double[0];
            // throw new ArrayConversionException("Cannot convert empty list");
        }

        Object first = data.get(0);

        if (first instanceof Number) {

            double[] result = new double[data.size()];

            for (int i = 0; i < data.size(); i++) {
                result[i] = ((Number) data.get(i)).doubleValue();
            }
            return result;

        } else {
            throw new ArrayConversionException("Data is not a number");
        }
    }

    /**
     * Convert the given data to a String[].
     * 
     * @return a String array
     */
    public String[] toStringArray() {

        String[] result = new String[data.size()];

        for (int i = 0; i < data.size(); i++) {
            result[i] = data.get(i).toString();
        }
        return result;

    }

    /**
     * Convert the given data to a Integer[].
     * 
     * @return an Integer array
     * @throws ArrayConversionException
     *             if the data are not Numbers
     */
    public Integer[] toIntegerObjectArray() throws ArrayConversionException {

        if (data.isEmpty()) {
            return new Integer[0];
            // throw new ArrayConversionException("Cannot convert empty list");
        }

        Object first = data.get(0);

        if (first instanceof Number) {

            Integer[] result = new Integer[data.size()];

            for (int i = 0; i < data.size(); i++) {
                result[i] = ((Number) data.get(i)).intValue();
            }
            return result;

        } else {
            throw new ArrayConversionException("Data is not a number");
        }

    }

    /**
     * Convert the given data to a int[].
     * 
     * @return an int array
     * @throws ArrayConversionException
     *             if the data are not Numbers
     */
    public int[] toIntArray() throws ArrayConversionException {

        if (data.isEmpty()) {
            return new int[0];
            // throw new ArrayConversionException("Cannot convert empty list");
        }

        Object first = data.get(0);

        if (first instanceof Number) {

            int[] result = new int[data.size()];

            for (int i = 0; i < data.size(); i++) {
                result[i] = ((Number) data.get(i)).intValue();
            }
            return result;

        } else {
            throw new ArrayConversionException("Data is not a number");
        }
    }

    /**
     * Convert the given data to a Float[].
     * 
     * @return a Float array
     * @throws ArrayConversionException
     *             if the data are not Numbers
     */
    public Float[] toFloatObjectArray() throws ArrayConversionException {

        if (data.isEmpty()) {
            return new Float[0];
            // throw new ArrayConversionException("Cannot convert empty list");
        }

        Object first = data.get(0);

        if (first instanceof Number) {

            Float[] result = new Float[data.size()];

            for (int i = 0; i < data.size(); i++) {
                result[i] = ((Number) data.get(i)).floatValue();
            }
            return result;

        } else {
            throw new ArrayConversionException("Data is not a number");
        }

    }

    /**
     * Convert the given data to a float[].
     * 
     * @return a float array
     * @throws ArrayConversionException
     *             if the data are not Numbers
     */
    public float[] toFloatArray() throws ArrayConversionException {
        if (data.isEmpty()) {
            return new float[0];
            // throw new ArrayConversionException("Cannot convert empty list");
        }

        Object first = data.get(0);

        if (first instanceof Number) {

            float[] result = new float[data.size()];

            for (int i = 0; i < data.size(); i++) {
                result[i] = ((Number) data.get(i)).floatValue();
            }
            return result;

        } else {
            throw new ArrayConversionException("Data is not a number");
        }
    }

    /**
     * Return as a list of Doubles.
     * @return a Double list
     * @throws ArrayConversionException if the conversion fails
     */
    public List<Double> toDoubleList() throws ArrayConversionException {

        List<Double> result = new ArrayList<Double>();

        Double[] arr = this.toDoubleObjectArray();

        for (Double d : arr) {
            result.add(d);
        }
        return result;

    }

    /**
     * Return as a list.
     * @return an Integer list
     * @throws ArrayConversionException  if the conversion fails
     */
    public List<Integer> toIntegerList() throws ArrayConversionException {

        List<Integer> result = new ArrayList<Integer>();

        Integer[] arr = this.toIntegerObjectArray();

        for (Integer d : arr) {
            result.add(d);
        }
        return result;

    }

    /**
     * Convert the data in this converter to a tab delimited string.
     * 
     * @return a tab delimited string
     */
    public String toString() {
        return toString("\t");
    }

    /**
     * Convert the data in this converter to a delimited string.
     * 
     * @param delimiter
     *            the separator between values
     * @return a delimited string
     */
    public String toString(String delimiter) {
        StringBuilder b = new StringBuilder();
        for (Object o : data) {
            b.append(o.toString() + delimiter);
        }
        return b.toString();
    }

    @SuppressWarnings("serial")
    public class ArrayConversionException extends Exception {

        public ArrayConversionException() {
            super();
        }

        public ArrayConversionException(String message) {
            super(message);
        }

        public ArrayConversionException(String message, Throwable cause) {
            super(message, cause);
        }

        public ArrayConversionException(Throwable cause) {
            super(cause);
        }
    }

}
