
import com.devebot.opflow.log4j.appenders.RabbitMQAppender;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoTest {
    
    private final static Logger LOG = LoggerFactory.getLogger(DemoTest.class);
    
    @Test
    public void testAListOfGeneratorObjects() {
        RabbitMQAppender myAppender = RabbitMQAppender.getActiveInstance();
        System.out.println("Identifier: " + myAppender.getIdentifier());
        if (LOG.isInfoEnabled()) {
            LOG.info("I'm here");
        }
    }
}
