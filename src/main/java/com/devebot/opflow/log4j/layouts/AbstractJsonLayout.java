package com.devebot.opflow.log4j.layouts;

import com.devebot.opflow.log4j.utils.JsonTool;
import java.util.Map;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

/**
 *
 * @author acegik
 */
public abstract class AbstractJsonLayout extends Layout {

    private Map<String, Object> metadata;
    protected int depth = 0;
    
    public AbstractJsonLayout() {
        super();
    }
    
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * format a given LoggingEvent to a JSON string
     *
     * @param loggingEvent the current logging event object
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
     * @param builder the JSON builder object
     * @param event the current logging event object
     */
    protected abstract void renderBasicFields(JsonTool.Builder builder, LoggingEvent event);

    /**
     * Converts LoggingEvent Throwable to JSON object
     *
     * @param builder the JSON builder object
     * @param event the current logging event object
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
     * @return boolean yes or no
     */
    @Override
    public boolean ignoresThrowable() {
        return false;
    }
}
