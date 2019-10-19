package com.devebot.opflow.log4j.layouts;

import com.devebot.opflow.log4j.utils.JsonTool;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

/**
 *
 * @author acegik
 */
public abstract class AbstractJsonLayout extends Layout {

    private Map<String, Object> metadata;
    private final Map<String, String> attrMapping;
    
    private String getAttrName(String attrName) {
        if (attrMapping.containsKey(attrName)) {
            return attrMapping.get(attrName);
        }
        return attrName;
    }

    public AbstractJsonLayout() {
        this(null);
    }
    
    public AbstractJsonLayout(Map<String, String> nameMapping) {
        super();
        if (nameMapping != null) {
            this.attrMapping = nameMapping;
        } else {
            this.attrMapping = new HashMap<>();
        }
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * format a given LoggingEvent to a JSON string
     *
     * @param loggingEvent
     * @return String representation of LoggingEvent
     */
    @Override
    public String format(LoggingEvent loggingEvent) {
        JsonTool.Builder builder = JsonTool.newBuilder();
        renderBasicFields(builder, loggingEvent);
        renderThrowableFields(builder, loggingEvent);
        if (metadata != null) {
            builder.putAll(metadata);
        }
        return builder.toString();
    }

    /**
     * Converts basic LoggingEvent properties to JSON object
     *
     * @param builder
     * @param event
     */
    protected abstract void renderBasicFields(JsonTool.Builder builder, LoggingEvent event);

    /**
     * Converts LoggingEvent Throwable to JSON object
     *
     * @param builder
     * @param event
     */
    protected abstract void renderThrowableFields(JsonTool.Builder builder, LoggingEvent event);
    
    /**
     * Just fulfilling the interface/abstract class requirements
     */
    @Override
    public void activateOptions() {
    }

    /**
     * Declares that this layout does not ignore Throwable if available
     *
     * @return
     */
    @Override
    public boolean ignoresThrowable() {
        return false;
    }
}
