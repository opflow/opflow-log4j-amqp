package com.devebot.opflow.log4j.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author drupalex
 */
public final class TypeConverter {

    private TypeConverter() {}

    public static <T> T convert(Object value, Class<T> type) {
        if (value == null) return null;

        if (type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }

        Method converter = CONVERTERS.get(value.getClass().getName() + "_" + type.getName());
        if (converter == null) {
            throw new IllegalArgumentException("Converter from " 
                + value.getClass().getName() + " to " + type.getName()
                + " has not implemented.");
        }

        try {
            return type.cast(converter.invoke(type, value));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot convert from " 
                + value.getClass().getName() + " to " + type.getName(), e);
        }
    }

    private static final Map<String, Method> CONVERTERS = new HashMap<String, Method>();

    static {
        Method[] methods = TypeConverter.class.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 1) {
                // Converter should accept 1 argument
                CONVERTERS.put(method.getParameterTypes()[0].getName() + "_"
                    + method.getReturnType().getName(), method);
            }
        }
    }

    private static Integer stringToInteger(String value) {
        return Integer.valueOf(value);
    }

    private static String integerToString(Integer value) {
        return value.toString();
    }
    
    private static Long stringToLong(String value) {
        return Long.valueOf(value);
    }
    
    private static String longToString(Long value) {
        return value.toString();
    }

    private static Boolean stringToBoolean(String value) {
        return Boolean.valueOf(value);
    }

    private static String booleanToString(Boolean value) {
        return value.toString();
    }

    private static Boolean integerToBoolean(Integer value) {
        return value == 0 ? Boolean.FALSE : Boolean.TRUE;
    }

    private static Integer booleanToInteger(Boolean value) {
        return value ? 1 : 0;
    }

    private static BigInteger integerToBigInteger(Integer value) {
        return BigInteger.valueOf(value);
    }

    private static Integer bigIntegerToInteger(BigInteger value) {
        return value.intValue();
    }

    private static BigDecimal doubleToBigDecimal(Double value) {
        return new BigDecimal(value);
    }

    private static Double bigDecimalToDouble(BigDecimal value) {
        return value.doubleValue();
    }
}
