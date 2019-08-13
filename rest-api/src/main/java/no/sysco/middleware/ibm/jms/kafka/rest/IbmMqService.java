package no.sysco.middleware.ibm.jms.kafka.rest;

import com.typesafe.config.ConfigFactory;
import java.util.logging.*;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

public class IbmMqService {
  private static final Level LOGLEVEL = Level.ALL;
  private static final Logger logger = Logger.getLogger(IbmMqService.class.getName());

  private String HOST; // = "localhost"; // Host name or IP address
  private int PORT; // = 1414; // Listener port for your queue manager
  private String CHANNEL; // = "DEV.APP.SVRCONN"; // Channel name
  private String QMGR; // = "QM1"; //System.getenv("QMGR"); // Queue manager name
  private String APP_USER; // = "app"; // User name that application uses to connect to MQ
  private String APP_PASSWORD; // = "passw0rd"; // Password that the application uses to connect to MQ
  private String QUEUE_NAME; // = "DEV.QUEUE.1"; // Queue that the application uses to put and get messages

  private JmsConnectionFactory jmsConnectionFactory;

  public IbmMqService(ApplicationConfig config) {
    this.HOST = config.host;
    this.PORT = config.port;
    this.CHANNEL = config.channelName;
    this.QMGR = config.queueManagerName;
    this.APP_USER = config.username;
    this.APP_PASSWORD = config.password;
    this.QUEUE_NAME = config.queueNameTarget;
    this.jmsConnectionFactory = createJMSConnectionFactory();

    setJMSProperties(jmsConnectionFactory);
    logger.info("created connection factory");
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

  private void recordFailure(Exception ex) {
    if (ex != null) {
      if (ex instanceof JMSException) {
        processJMSException((JMSException) ex);
      } else {
        logger.warning(ex.getMessage());
      }
    }
    logger.warning("FAILURE");
    return;
  }

  private void processJMSException(JMSException jmsex) {
    logger.warning(jmsex.getMessage());
    Throwable innerException = jmsex.getLinkedException();
    if (innerException != null) {
      logger.warning("Inner exception(s):");
    }
    while (innerException != null) {
      logger.warning(innerException.getMessage());
      innerException = innerException.getCause();
    }
    return;
  }

  private void setJMSProperties(JmsConnectionFactory cf) {
    try {
      cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
      cf.setIntProperty(WMQConstants.WMQ_PORT, PORT);
      cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
      cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
      cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QMGR);
      //cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "JmsPut (JMS)");
      //cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
      cf.setStringProperty(WMQConstants.USERID, APP_USER);
      cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);
    } catch (JMSException jmsex) {
      recordFailure(jmsex);
    }
    return;
  }

  public JmsConnectionFactory getJmsConnectionFactory() {
    return jmsConnectionFactory;
  }
// use to produce msg
  public static void main(String[] args) {
    final IbmMqService ibmMqService =
        new IbmMqService(ApplicationConfig.load(ConfigFactory.load()));

    logger.info("Put application is starting");

    JMSContext context = null;
    Destination destination = null;
    JMSProducer producer = null;

    context = ibmMqService.getJmsConnectionFactory().createContext();
    logger.info("context created");
    destination = context.createQueue("queue:///" + ibmMqService.QUEUE_NAME);
    logger.info("destination created");
    producer = context.createProducer();
    logger.info("producer created");
    for (int i = 1; i <= 1000; i++) {
      TextMessage message = context.createTextMessage("This is message number " + i + ".");
      producer.send(destination, message);
    }
    logger.info("Sent all messages!");
  }
}
