package com.devebot.opflow.log4j.utils;

import java.util.Map;
import java.util.Properties;

/**
 *
 * @author pnhung177
 */
public class PropTool {
    
    public static Properties filterProperties(Properties source, String filter) {
        return filterProperties(source, new String[] { filter });
    }
    
    public static Properties filterProperties(Properties source, String[] filters) {
        Properties result = new Properties();
        if (source == null) {
            return null;
        }
        if (filters == null || filters.length == 0) {
            return new Properties(source);
        }
        for(Map.Entry<Object, Object> entry: source.entrySet()) {
            String key = entry.getKey().toString();
            for (String filter : filters) {
                if (key.startsWith(filter)) {
                    result.put(key, source.get(key));
                    break;
                }
            }
        }
        return result;
    }
}
