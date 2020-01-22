package com.devebot.opflow.log4j.appenders;

import com.devebot.opflow.log4j.layouts.AbstractJsonLayout;
import com.devebot.opflow.log4j.utils.JsonTool;
import com.devebot.opflow.log4j.utils.PropTool;
import com.devebot.opflow.log4j.utils.TypeConverter;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RabbitMQAppender extends AppenderSkeleton {
    
    private final ConnectionFactory factory = new ConnectionFactory();
    private Connection connection = null;
    private Channel channel = null;

    private String identifier = null;
    private String host = "localhost";
    private int port = 5762;
    private String username = "guest";
    private String password = "guest";
    private String virtualHost = "/";

    private String exchangeName = "log-exchange";
    private String exchangeType = "direct";
    private boolean exchangeDurable = false;
    private String queueName = "log-queue";
    private boolean queueDurable = false;
    private boolean queueExclusive = false;
    private boolean queueAutoDelete = false;
    private long queueMaxLength = 0;
    private long queueMaxLengthBytes = 0;
    private long queueMessageTtl = 0;
    private String routingKey = "";
    private Map<String, Object> metadata;

    private Boolean automaticRecoveryEnabled = true;
    private Boolean topologyRecoveryEnabled = true;
    private int networkRecoveryInterval = 0;
    private int connectionTimeout = 0;
    private int handshakeTimeout = 0;
    private int shutdownTimeout = 0;
    private int heartbeatTimeout = 0;
    private int frameSizeLimit = 0;

    private ExecutorService threadPool = null;
    private static RabbitMQAppender instance;

    public RabbitMQAppender() {
        super();
        instance = this;
    }

    public static RabbitMQAppender getActiveInstance() {
        return instance;
    }

    /**
     * Creates the connection, channel, declares the exchangeName and queueName
     * 
     * @see AppenderSkeleton#activateOptions()
     */
    @Override
    public void activateOptions() {
        super.activateOptions();

        this.updateInlineOptions();

        try {
            this.getConnection();
        } catch (IOException | TimeoutException ioe) {
            errorHandler.error(ioe.getMessage(), ioe, ErrorCode.GENERIC_FAILURE);
        }

        try {
            this.getChannel();
        } catch (IOException | TimeoutException ioe) {
            errorHandler.error(ioe.getMessage(), ioe, ErrorCode.GENERIC_FAILURE);
        }
        
        try {
            this.assertExchange();
        } catch (IOException | TimeoutException ioe) {
            errorHandler.error(ioe.getMessage(), ioe, ErrorCode.GENERIC_FAILURE);
        }

        try {
            this.assertQueue();
        } catch (IOException | TimeoutException ioe) {
            errorHandler.error(ioe.getMessage(), ioe, ErrorCode.GENERIC_FAILURE);
        }
        
        if (layout instanceof AbstractJsonLayout) {
            AbstractJsonLayout jsonLayout = (AbstractJsonLayout) layout;
            try {
                jsonLayout.setMetadata(metadata);
            } catch (JsonSyntaxException jse) {
                errorHandler.error(jse.getMessage(), jse, ErrorCode.GENERIC_FAILURE);
            }
        }
    }

    /**
     * Ensures that a Layout property is required
     * 
     * @return boolean Layout is required if true
     */
    @Override
    public boolean requiresLayout() {
        return true;
    }

    /**
     * Submits LoggingEvent for publishing if it reaches severity threshold.
     * 
     * @param loggingEvent the logging event object
     */
    @Override
    protected void append(LoggingEvent loggingEvent) {
        if ( isAsSevereAsThreshold(loggingEvent.getLevel())) {
            getThreadPoolExecutor().submit(new LoggingTask(loggingEvent));
        }
    }

    /**
     * Simple Callable class that publishes messages to RabbitMQ server
     */
    class LoggingTask implements Callable<LoggingEvent> {

        LoggingEvent loggingEvent;

        LoggingTask(LoggingEvent loggingEvent) {
            this.loggingEvent = loggingEvent;
        }

        /**
         * Method is called by the ExecutorService and publishes the message to 
         *  the RabbitMQ Server
         * 
         * @return LoggingEvent the current logging event object
         * @throws Exception
         */
        @Override
        public LoggingEvent call() throws Exception {
            AMQP.BasicProperties.Builder b = new AMQP.BasicProperties().builder()
                    .appId(identifier)
                    .correlationId(String.format("%s:%s", identifier, System.currentTimeMillis()))
                    .type(loggingEvent.getLevel().toString())
                    .contentType("text/json");
            String message = layout.format(loggingEvent);
            getChannel().basicPublish(exchangeName, routingKey, b.build(), message.getBytes());
            return loggingEvent;
        }
    }

    private void updateInlineOptions() {
        String prefix = "log4j.appender." + this.getName() + ".";
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
                            fieldSetter.invoke(this, fieldArg);
                        }
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                errorHandler.error(e.getMessage(), e, ErrorCode.GENERIC_FAILURE);
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
            return this.getClass().getDeclaredMethod("get" + fieldName);
        }
        catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }
    
    private Method extractFieldSetter(String fieldName, Class fieldType) {
        try {
            return this.getClass().getDeclaredMethod("set" + fieldName, fieldType);
        }
        catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }
    
    /*
     * The getters & setters corresponding to appender configuration properties
     */

    public String getIdentifier() {
        return identifier;
    }


    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }


    public String getHost() {
        return host;
    }


    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    
    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getExchange() {
        return exchangeName;
    }

    public void setExchange(String exchange) {
        this.exchangeName = exchange;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(String type) {
        this.exchangeType = type;
    }

    public boolean isExchangeDurable() {
        return exchangeDurable;
    }

    public void setExchangeDurable(boolean durable) {
        this.exchangeDurable = durable;
    }

    public String getQueue() {
        return queueName;
    }

    public void setQueue(String queue) {
        this.queueName = queue;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public boolean isQueueDurable() {
        return queueDurable;
    }

    public void setQueueDurable(boolean queueDurable) {
        this.queueDurable = queueDurable;
    }

    public boolean isQueueExclusive() {
        return queueExclusive;
    }

    public void setQueueExclusive(boolean queueExclusive) {
        this.queueExclusive = queueExclusive;
    }

    public boolean isQueueAutoDelete() {
        return queueAutoDelete;
    }

    public void setQueueAutoDelete(boolean queueAutoDelete) {
        this.queueAutoDelete = queueAutoDelete;
    }

    public long getQueueMaxLength() {
        return queueMaxLength;
    }

    public void setQueueMaxLength(long queueMaxLength) {
        this.queueMaxLength = queueMaxLength;
    }

    public long getQueueMaxLengthBytes() {
        return queueMaxLengthBytes;
    }

    public void setQueueMaxLengthBytes(long queueMaxLengthBytes) {
        this.queueMaxLengthBytes = queueMaxLengthBytes;
    }

    public long getQueueMessageTtl() {
        return queueMessageTtl;
    }

    public void setQueueMessageTtl(long queueMessageTtl) {
        this.queueMessageTtl = queueMessageTtl;
    }

    public long getQueueMessageTTL() {
        return queueMessageTtl;
    }

    public void setQueueMessageTTL(long queueMessageTTL) {
        this.queueMessageTtl = queueMessageTTL;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
    
    public String getMetadata() {
        return JsonTool.toString(metadata);
    }

    public void setMetadata(String metadataString) {
        this.metadata = JsonTool.toJsonMap(metadataString);
    }

    public Boolean getAutomaticRecoveryEnabled() {
        return automaticRecoveryEnabled;
    }

    public void setAutomaticRecoveryEnabled(Boolean automaticRecoveryEnabled) {
        this.automaticRecoveryEnabled = automaticRecoveryEnabled;
    }

    public Boolean getTopologyRecoveryEnabled() {
        return topologyRecoveryEnabled;
    }

    public void setTopologyRecoveryEnabled(Boolean topologyRecoveryEnabled) {
        this.topologyRecoveryEnabled = topologyRecoveryEnabled;
    }

    public int getNetworkRecoveryInterval() {
        return networkRecoveryInterval;
    }

    public void setNetworkRecoveryInterval(int networkRecoveryInterval) {
        this.networkRecoveryInterval = networkRecoveryInterval;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getHandshakeTimeout() {
        return handshakeTimeout;
    }

    public void setHandshakeTimeout(int handshakeTimeout) {
        this.handshakeTimeout = handshakeTimeout;
    }

    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(int shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    public int getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public void setHeartbeatTimeout(int heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public int getRequestedHeartbeat() {
        return heartbeatTimeout;
    }

    public void setRequestedHeartbeat(int requestedHeartbeat) {
        this.heartbeatTimeout = requestedHeartbeat;
    }

    public int getFrameSizeLimit() {
        return frameSizeLimit;
    }

    public void setFrameSizeLimit(int frameSizeLimit) {
        this.frameSizeLimit = frameSizeLimit;
    }

    public int getRequestedFrameMax() {
        return frameSizeLimit;
    }

    public void setRequestedFrameMax(int requestedFrameMax) {
        this.frameSizeLimit = requestedFrameMax;
    }

    /**
     * Declares the exchangeName to publish the log messages
     * @throws IOException
     */
    private void assertExchange() throws IOException, TimeoutException {
        final Channel _channel = getChannel();
        synchronized (_channel) {
            _channel.exchangeDeclare(exchangeName, exchangeType, exchangeDurable);
        }
    }


    /**
     * Declares and binds a queueName to store the published log messages
     * @throws IOException
     * @throws TimeoutException
     */
    private void assertQueue() throws IOException, TimeoutException {
        Map<String, Object> args = JsonTool.newBuilder().toMap();
        if (queueMaxLength > 0) {
            args.put("x-max-length", queueMaxLength);
        }
        if (queueMaxLengthBytes > 0) {
            args.put("x-max-length-bytes", queueMaxLengthBytes);
        }
        if (queueMessageTtl > 0) {
            args.put("x-message-ttl", queueMessageTtl);
        }
        final Channel _channel = getChannel();
        synchronized (_channel) {
            _channel.queueDeclare(queueName, queueDurable, queueExclusive, queueAutoDelete, args);
            _channel.queueBind(queueName, exchangeName, routingKey);
        }
    }

    /**
     * Creates a single channel to RabbitMQ server
     * @return Channel the activated channel object
     * @throws IOException
     */
    private Channel getChannel() throws IOException, TimeoutException {
        if (this.channel == null || !this.channel.isOpen()) {
            this.channel = getConnection().createChannel();
        }
        return this.channel;
    }

    /**
     * Creates a single connection to RabbitMQ server according to properties
     * @return Connection the activated connection object
     * @throws IOException
     */
    private Connection getConnection() throws IOException, TimeoutException {
        if (this.connection == null || !this.connection.isOpen()) {
            this.connection = initConnectionFactory().newConnection();
        }
        return this.connection;
    }

    /**
     * Sets the ConnectionFactory parameters
     */
    private ConnectionFactory initConnectionFactory() {
        factory.setHost(this.host);
        factory.setPort(this.port);
        factory.setVirtualHost(this.virtualHost);
        factory.setUsername(this.username);
        factory.setPassword(this.password);
        
        if (automaticRecoveryEnabled != null) {
            factory.setAutomaticRecoveryEnabled(automaticRecoveryEnabled);
        }
        if (topologyRecoveryEnabled != null) {
            factory.setTopologyRecoveryEnabled(topologyRecoveryEnabled);
        }
        if (networkRecoveryInterval > 0) {
            factory.setNetworkRecoveryInterval(networkRecoveryInterval);
        }
        if (connectionTimeout > 0) {
            factory.setConnectionTimeout(connectionTimeout);
        }
        if (handshakeTimeout > 0) {
            factory.setHandshakeTimeout(handshakeTimeout);
        }
        if (heartbeatTimeout > 0) {
            factory.setRequestedHeartbeat(heartbeatTimeout);
        }
        if (frameSizeLimit > 0) {
            factory.setRequestedFrameMax(frameSizeLimit);
        }
        
        return factory;
    }

    private ExecutorService getThreadPoolExecutor() {
        if (threadPool == null) {
            threadPool = Executors.newSingleThreadExecutor();
        }
        return threadPool;
    }
    
    /**
     * Closes the channel and connection to RabbitMQ when shutting down the appender
     */
    @Override
    public void close() {
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                    threadPool.awaitTermination(60, TimeUnit.SECONDS);
                }
            } catch (InterruptedException ie) {
                threadPool.shutdownNow();
            }
            threadPool = null;
        }

        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException | TimeoutException ioe) {
                errorHandler.error(ioe.getMessage(), ioe, ErrorCode.CLOSE_FAILURE);
            } finally {
                channel = null;
            }
        }

        if (connection != null && connection.isOpen()) {
            try {
                connection.close();
            } catch (IOException ioe) {
                errorHandler.error(ioe.getMessage(), ioe, ErrorCode.CLOSE_FAILURE);
            } finally {
                connection = null;
            }
        }
    }
}
