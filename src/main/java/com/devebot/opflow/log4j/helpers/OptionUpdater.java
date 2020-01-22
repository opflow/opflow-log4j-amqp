package com.devebot.opflow.log4j.helpers;

import com.devebot.opflow.log4j.utils.PropTool;
import com.devebot.opflow.log4j.utils.TypeConverter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;

/**
 *
 * @author drupalex
 */
public class OptionUpdater {
    private final AppenderSkeleton target;

    public OptionUpdater(AppenderSkeleton target) {
        this.target = target;
    }

    public void activateOptions() {
        String prefix = "log4j.appender." + target.getName() + ".";
        Properties inlineOpts = PropTool.filterProperties(System.getProperties(), prefix);

        for (Map.Entry<Object, Object> entry : inlineOpts.entrySet()) {
            try {
                String fieldName = extractFieldName(prefix, entry.getKey().toString());
                Method fieldGetter = extractFieldGetter(fieldName);
                if (fieldGetter != null) {
                    Class fieldType = fieldGetter.getReturnType();
                    Method fieldSetter = extractFieldSetter(fieldName, fieldType);
                    if (fieldSetter != null) {
                        Object fieldArg = TypeConverter.convert(entry.getValue(), fieldType);
                        if (fieldArg != null) {
                            fieldSetter.invoke(target, fieldArg);
                        }
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                target.getErrorHandler().error(e.getMessage(), e, ErrorCode.GENERIC_FAILURE);
            }
        }
    }

    private String extractFieldName(String prefix, String propKey) {
        if (propKey != null) {
            return capitalize(propKey.replace(prefix, ""));
        }
        return propKey;
    }

    private String capitalize(String str) {
        if (str != null && str.length() > 0) {
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
        return str;
    }

    private Method extractFieldGetter(String fieldName) {
        try {
            return target.getClass().getDeclaredMethod("get" + fieldName);
        }
        catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    private Method extractFieldSetter(String fieldName, Class fieldType) {
        try {
            return target.getClass().getDeclaredMethod("set" + fieldName, fieldType);
        }
        catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }
}
