package com.devebot.opflow.log4j.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author drupalex
 */
public final class TypeConverter {

    private TypeConverter() {}

    public static <T> T convert(Object value, Class<T> targetType) {
        if (value == null) return unwrap(targetType, null);
        
        Class<T> type = wrap(targetType);
        
        if (type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }

        Method converter = CONVERTERS.get(value.getClass().getName() + "_" + type.getName());
        if (converter == null) {
            throw new IllegalArgumentException(MessageFormat.format("Converter from {0} to {1} has not implemented.",
                    new Object[] {
                            value.getClass().getName(), type.getName()
                    }));
        }

        try {
            return unwrap(targetType, type.cast(converter.invoke(type, value)));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(MessageFormat.format("Cannot convert from {0} to {1}.",
                    new Object[] {
                            value.getClass().getName(), type.getName()
                    }), e);
        }
    }

    private static final Map<String, Method> CONVERTERS = new HashMap<String, Method>();
    private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS = new HashMap<>();
    private static final Map<Class<?>, Object> PRIMITIVES_NULL_VALUES = new HashMap<>();
    
    static {
        Method[] methods = TypeConverter.class.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 1 && method.getName().startsWith("transform_")) {
                // Converter should accept 1 argument
                CONVERTERS.put(method.getParameterTypes()[0].getName() + "_"
                    + method.getReturnType().getName(), method);
            }
        }
        // initialize the PRIMITIVES_TO_WRAPPERS mappings
        PRIMITIVES_TO_WRAPPERS.put(boolean.class, Boolean.class);
        PRIMITIVES_TO_WRAPPERS.put(byte.class, Byte.class);
        PRIMITIVES_TO_WRAPPERS.put(char.class, Character.class);
        PRIMITIVES_TO_WRAPPERS.put(double.class, Double.class);
        PRIMITIVES_TO_WRAPPERS.put(float.class, Float.class);
        PRIMITIVES_TO_WRAPPERS.put(int.class, Integer.class);
        PRIMITIVES_TO_WRAPPERS.put(long.class, Long.class);
        PRIMITIVES_TO_WRAPPERS.put(short.class, Short.class);
        PRIMITIVES_TO_WRAPPERS.put(void.class, Void.class);
        // initialize the PRIMITIVES_NULL_VALUES mappings
        PRIMITIVES_NULL_VALUES.put(boolean.class, Boolean.FALSE);
        PRIMITIVES_NULL_VALUES.put(byte.class, (byte) 0);
        PRIMITIVES_NULL_VALUES.put(char.class, (char) 0);
        PRIMITIVES_NULL_VALUES.put(double.class, (double) 0);
        PRIMITIVES_NULL_VALUES.put(float.class, (int) 0);
        PRIMITIVES_NULL_VALUES.put(int.class, (int) 0);
        PRIMITIVES_NULL_VALUES.put(long.class, (long) 0);
        PRIMITIVES_NULL_VALUES.put(short.class, (short) 0);
        PRIMITIVES_NULL_VALUES.put(void.class, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> wrap(Class<T> c) {
        return c.isPrimitive() ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
    }
    
    private static <T> T unwrap(Class<T> c, T value) {
        if (value == null) {
            return PRIMITIVES_NULL_VALUES.containsKey(c) ? (T) PRIMITIVES_NULL_VALUES.get(c) : value;
        }
        return value;
    }

    private static Integer transform_stringToInteger(String value) {
        return Integer.valueOf(value);
    }

    private static String transform_integerToString(Integer value) {
        return value.toString();
    }
    
    private static Long transform_stringToLong(String value) {
        return Long.valueOf(value);
    }
    
    private static String transform_longToString(Long value) {
        return value.toString();
    }

    private static Float transform_stringToFloat(String value) {
        return Float.valueOf(value);
    }
    
    private static String transform_floatToString(Float value) {
        return value.toString();
    }
    
    private static Double transform_stringToDouble(String value) {
        return Double.valueOf(value);
    }
    
    private static String transform_doubleToString(Double value) {
        return value.toString();
    }
    
    private static Boolean transform_stringToBoolean(String value) {
        return Boolean.valueOf(value);
    }

    private static String transform_booleanToString(Boolean value) {
        return value.toString();
    }

    private static Boolean transform_integerToBoolean(Integer value) {
        return value == 0 ? Boolean.FALSE : Boolean.TRUE;
    }

    private static Integer transform_booleanToInteger(Boolean value) {
        return value ? 1 : 0;
    }

    private static BigInteger transform_integerToBigInteger(Integer value) {
        return BigInteger.valueOf(value);
    }

    private static Integer transform_bigIntegerToInteger(BigInteger value) {
        return value.intValue();
    }

    private static BigDecimal transform_doubleToBigDecimal(Double value) {
        return new BigDecimal(value);
    }

    private static Double transform_bigDecimalToDouble(BigDecimal value) {
        return value.doubleValue();
    }
}
