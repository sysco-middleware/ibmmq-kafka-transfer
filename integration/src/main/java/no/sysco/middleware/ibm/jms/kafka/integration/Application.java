package no.sysco.middleware.ibm.jms.kafka.integration;

import com.typesafe.config.ConfigFactory;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

// example: https://github.com/ibm-messaging/mq-dev-patterns/blob/master/JMS/com/ibm/mq/samples/jms/BasicConsumer.java
public class Application {
  private static final Logger logger = Logger.getLogger(Application.class.getName());
  public static void main(String[] args) {
    final ApplicationConfig config = ApplicationConfig.load(ConfigFactory.load());
    final ConnectionHelper connectionHelper = new ConnectionHelper(config);
    final KafkaProducer<String, String> stringStringKafkaProducer =
        new KafkaProducer<>(producerProperties());

    start(connectionHelper,stringStringKafkaProducer);
  }

  // business logic
  public static void start(ConnectionHelper connectionHelper, KafkaProducer<String, String> producer) {
    final Destination queue = connectionHelper.getDestination();
    final JMSConsumer consumer = connectionHelper.getContext().createConsumer(queue);
    logger.info("consumer created");
    final String topic = "topic-1";
    while (true) {
      try {
        Message receivedMessage = consumer.receive();

        if (receivedMessage instanceof TextMessage) {
          TextMessage textMessage = (TextMessage) receivedMessage;
          try {
            final String payload = textMessage.getText();
            logger.info("Received message: " + payload);
            producer.send(new ProducerRecord<>(topic, null, payload),
                (recordMetadata, e) -> {
                  if (e==null) {
                    try {
                      receivedMessage.acknowledge();
                    } catch (JMSException ex) {
                      ex.printStackTrace();
                      int i = 5/0; // :)
                    }
                  }
                });
          } catch (JMSException jmsex) {
            //recordFailure(jmsex);
            logger.info("recordFailure(jmsex);");
          }
        } else if (receivedMessage instanceof Message) {
          logger.info("Message received was not of type TextMessage.\n");
        } else {
          logger.info("Received object not of JMS Message type!\n");
        }

      } catch (JMSRuntimeException jmsex) {
        jmsex.printStackTrace();
        //try {
        //  //Thread.sleep(1000);
        //} catch (InterruptedException e) {
        //}
      }
    }
  }

  public static Properties producerProperties() {
    final Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    properties.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return properties;

  }
}
