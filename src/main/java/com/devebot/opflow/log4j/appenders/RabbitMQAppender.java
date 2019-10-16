package com.devebot.opflow.log4j.appenders;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private String routingKey = "";

    private ExecutorService threadPool = null;

    public RabbitMQAppender() {
        super();
    }

    /**
     * Creates the connection, channel, declares the exchangeName and queueName
     * 
     * @see AppenderSkeleton#activateOptions()
     */
    @Override
    public void activateOptions() {
        super.activateOptions();

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
    }

    /**
     * Ensures that a Layout property is required
     * 
     * @return
     */
    @Override
    public boolean requiresLayout() {
        return true;
    }

    /**
     * Submits LoggingEvent for publishing if it reaches severity threshold.
     * 
     * @param loggingEvent
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
         * @return
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

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
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
        final Channel _channel = getChannel();
        synchronized (_channel) {
            _channel.queueDeclare(queueName, queueDurable, queueExclusive, queueAutoDelete, null);
            _channel.queueBind(queueName, exchangeName, routingKey);
        }
    }

    /**
     * Creates a single channel to RabbitMQ server
     * @return
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
     * @return
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
