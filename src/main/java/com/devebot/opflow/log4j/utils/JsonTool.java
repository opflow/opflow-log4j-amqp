package com.devebot.opflow.log4j.utils;

import com.google.gson.nostro.Gson;
import com.google.gson.nostro.GsonBuilder;
import com.google.gson.nostro.JsonArray;
import com.google.gson.nostro.JsonSyntaxException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author drupalex
 */
public class JsonTool {
    private static final Gson GSON = new Gson();
    private static final Gson PSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static String toString(Object jsonObj) {
        return toString(jsonObj, false);
    }
    
    public static String toString(Object jsonObj, boolean pretty) {
        return pretty ? PSON.toJson(jsonObj) : GSON.toJson(jsonObj);
    }
    
    public static String toString(Object[] objs, Type[] types) {
        return toString(objs, types, false);
    }
    
    public static String toString(Object[] objs, Type[] types, boolean pretty) {
        JsonArray array = new JsonArray();
        for(int i=0; i<objs.length; i++) {
            array.add(GSON.toJson(objs[i], types[i]));
        }
        return pretty ? PSON.toJson(array) : GSON.toJson(array);
    }
    
    public static String toString(Map<String, Object> jsonMap) {
        return toString(jsonMap, false);
    }
    
    public static String toString(Map<String, Object> jsonMap, boolean pretty) {
        return pretty ? PSON.toJson(jsonMap) : GSON.toJson(jsonMap);
    }
    
    public static Map<String, Object> toJsonMap(String json) {
        try {
            Map<String,Object> map = GSON.fromJson(json, Map.class);
            return map;
        } catch (JsonSyntaxException e) {
            throw e;
        }
    }
    
    public static Builder newBuilder() {
        return JsonTool.newBuilder(null, null);
    }
    
    public static Builder newBuilder(Listener listener) {
        return JsonTool.newBuilder(listener, null);
    }
    
    public static Builder newBuilder(Map<String, Object> defaultOpts) {
        return JsonTool.newBuilder(null, defaultOpts);
    }
    
    public static Builder newBuilder(Listener listener, Map<String, Object> defaultOpts) {
        Map<String, Object> source = new LinkedHashMap<>();
        if (defaultOpts != null) {
            source.putAll(defaultOpts);
        }
        if (listener != null) {
            listener.transform(source);
        }
        return new Builder(source);
    }
    
    public interface Listener {
        public void transform(Map<String, Object> opts);
    }
    
    public static class Builder {
        private final Map<String, Object> fields;
        
        public Builder() {
            this(null);
        }
        
        public Builder(Map<String, Object> source) {
            fields = (source == null) ? new LinkedHashMap<String, Object>() : source;
        }
        
        public Builder put(String key, Object value) {
            fields.put(key, value);
            return this;
        }
        
        public Builder putAll(Map<String, Object> source) {
            if (source != null) {
                fields.putAll(source);
            }
            return this;
        }

        public Map<String, Object> toMap() {
            return fields;
        }
        
        @Override
        public String toString() {
            return toString(false);
        }
        
        public String toString(boolean pretty) {
            return JsonTool.toString(fields, pretty);
        }
    }
}
