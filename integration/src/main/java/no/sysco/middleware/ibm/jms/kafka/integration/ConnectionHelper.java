package no.sysco.middleware.ibm.jms.kafka.integration;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;

public class ConnectionHelper {
  private static final Logger logger = Logger.getLogger(ConnectionHelper.class.getName());

  // Create variables for the connection to MQ
  private String HOST; // Host name or IP address
  private int PORT; // Listener port for your queue manager
  private String CHANNEL; // Channel name
  private String QMGR; // Queue manager name
  private String APP_USER; // User name that application uses to connect to MQ
  private String APP_PASSWORD; // Password that the application uses to connect to MQ
  private String QUEUE_NAME; // Queue that the application uses to put and get messages to and from
  //private static String TOPIC_NAME; // Topic that the application publishes to

  JMSContext context;

  public ConnectionHelper (ApplicationConfig config) {

    mqConnectionVariables(config);
    JmsConnectionFactory connectionFactory = createJMSConnectionFactory();
    setJMSProperties(connectionFactory);

    context = connectionFactory.createContext();
    logger.info("context created");

  }

  public JMSContext getContext () {
    return context;
  }

  public void closeContext () {
    context.close();
    context = null;
  }

  public Destination getDestination () {
    return context.createQueue("queue:///" + QUEUE_NAME);
  }

  //public Destination getTopicDestination () {
  //  return context.createTopic("topic://" + TOPIC_NAME);
  //}

  private void mqConnectionVariables(ApplicationConfig config) {
    HOST = config.host;
    PORT = config.port;
    CHANNEL = config.channelName;
    QMGR = config.queueManagerName;
    APP_USER = config.username;
    APP_PASSWORD = config.password;
    QUEUE_NAME = config.queueNameTarget;
  }

  private JmsConnectionFactory createJMSConnectionFactory() {
    JmsFactoryFactory ff;
    JmsConnectionFactory cf;
    try {
      ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
      cf = ff.createConnectionFactory();
    } catch (JMSException jmsex) {
      recordFailure(jmsex);
      cf = null;
    }
    return cf;
  }

  private void setJMSProperties(JmsConnectionFactory cf) {
    try {
      cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
      cf.setIntProperty(WMQConstants.WMQ_PORT, PORT);
      cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
      cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
      cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QMGR);
      cf.setIntProperty(WMQConstants.ACKNOWLEDGE_MODE, WMQConstants.CLIENT_ACKNOWLEDGE);
      //cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "JmsPut (JMS)");
      //cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
      cf.setStringProperty(WMQConstants.USERID, APP_USER);
      cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);
    } catch (JMSException jmsex) {
      recordFailure(jmsex);
    }
    return;
  }

  private static void recordFailure(Exception ex) {
    if (ex != null) {
      if (ex instanceof JMSException) {
        processJMSException((JMSException) ex);
      } else {
        logger.warning(ex.getMessage());
      }
    }
    System.out.println("FAILURE");
    return;
  }

  private static void processJMSException(JMSException jmsex) {
    logger.info(jmsex.getMessage());
    Throwable innerException = jmsex.getLinkedException();
    logger.info("Exception is: " + jmsex);
    if (innerException != null) {
      logger.info("Inner exception(s):");
    }
    while (innerException != null) {
      logger.warning(innerException.getMessage());
      innerException = innerException.getCause();
    }
    return;
  }
}
