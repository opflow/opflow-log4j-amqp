package com.devebot.opflow.log4j.layouts;

import com.devebot.opflow.log4j.utils.JsonTool;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LogstashLayout extends AbstractJsonLayout {

    public LogstashLayout() {
        super();
    }

    @Override
    protected void renderBasicFields(JsonTool.Builder builder, LoggingEvent event) {
        builder.put("@timestamp", event.getTimeStamp());
        builder.put("level", event.getLevel().toString());
        builder.put("message", event.getMessage());
        builder.put("loggerName", event.getLoggerName());
        builder.put("threadName", event.getThreadName());
        builder.put("@version", 1);
    }

    @Override
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
}
