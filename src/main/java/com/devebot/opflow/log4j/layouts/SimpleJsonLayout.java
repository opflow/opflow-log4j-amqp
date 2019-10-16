package com.devebot.opflow.log4j.layouts;

import com.devebot.opflow.log4j.utils.JsonTool;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SimpleJsonLayout extends Layout {

    /**
     * format a given LoggingEvent to a JSON string
     *
     * @param loggingEvent
     * @return String representation of LoggingEvent
     */
    @Override
    public String format(LoggingEvent loggingEvent) {
        JsonTool.Builder builder = JsonTool.newBuilder();

        renderNormalFields(builder, loggingEvent);
        renderThrowableFields(builder, loggingEvent);

        return builder.toString();
    }

    /**
     * Converts basic LoggingEvent properties to JSON object
     *
     * @param builder
     * @param event
     */
    protected void renderNormalFields(JsonTool.Builder builder, LoggingEvent event) {
        builder.put("timestamp", event.getTimeStamp());
        builder.put("level", event.getLevel().toString());
        builder.put("message", event.getMessage());
        builder.put("logger", event.getLoggerName());
        builder.put("threadName", event.getThreadName());
    }

    /**
     * Converts LoggingEvent Throwable to JSON object
     *
     * @param builder
     * @param event
     */
    protected void renderThrowableFields(JsonTool.Builder builder, LoggingEvent event) {
        ThrowableInformation info = event.getThrowableInformation();
        if (info != null) {
            final Throwable t = info.getThrowable();

            Map<String, Object> throwable = new LinkedHashMap<>();
            throwable.put("message", t.getMessage());
            throwable.put("className", t.getClass().getCanonicalName());
            builder.put("throwable", throwable);

            List<Map<String, Object>> traceObjects = new ArrayList<>();
            for (StackTraceElement ste : t.getStackTrace()) {
                Map<String, Object> element = new LinkedHashMap<>();
                element.put("class", ste.getClassName());
                element.put("method", ste.getMethodName());
                element.put("line", ste.getLineNumber());
                element.put("file", ste.getFileName());
                traceObjects.add(element);
            }
            builder.put("stackTrace", traceObjects);
        }
    }

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
