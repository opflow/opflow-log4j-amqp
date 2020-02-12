package com.devebot.opflow.log4j.layouts;

import com.devebot.opflow.log4j.utils.JsonTool;
import com.devebot.opflow.log4j.utils.TimeUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class LogstashLayout extends AbstractJsonLayout {

    public LogstashLayout() {
        super();
    }

    @Override
    protected void renderBasicFields(JsonTool.Builder builder, LoggingEvent event) {
        builder.put("logId", UUID.randomUUID().toString());
        builder.put("logTime", event.getTimeStamp());
        if (this.depth >= 1) {
            builder.put("timestamp", TimeUtil.toISO8601UTC(event.getTimeStamp()));
        }
        builder.put("level", event.getLevel().toString());
        builder.put("message", event.getMessage());
        builder.put("loggerName", event.getLoggerName());
        if (this.depth >= 1) {
            builder.put("threadName", event.getThreadName());
        }
        if (this.depth >= 2) {
            LocationInfo li = event.getLocationInformation();
            builder.put("className", li.getClassName());
            builder.put("methodName", li.getMethodName());
            builder.put("lineNumber", li.getLineNumber());
            if (this.depth >= 3) {
                builder.put("fileName", li.getFileName());
            }
        }
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
