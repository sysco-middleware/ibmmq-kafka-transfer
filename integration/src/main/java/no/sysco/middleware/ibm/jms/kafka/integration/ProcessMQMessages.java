package no.sysco.middleware.ibm.jms.kafka.integration;

import com.ibm.mq.constants.MQPropertyIdentifiers;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.util.Properties;
import java.util.UUID;

// Ref: => https://github.com/ibm-messaging/mq-ibmcloud/blob/master/cloudfunctions/javaAction/src/main/java/ProcessMQMessages.java

public class ProcessMQMessages {
    private static final Logger logger = LoggerFactory.getLogger(ProcessMQMessages.class);
    /**
     * IBM Cloud Functions (OpenWhisk) Action that processes messages from a queue
     * on an IBM MQ queue manager.
     *
     */
    public static void main(String[] args) {


        final KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(getKafkaProducerProperties());

        // kafka
        final String topic = "test-4";

        // queue manager.
        String PARAM_QUEUE_NAME = "DEV.QUEUE.1";
        String PARAM_USERNAME = "app";
        String PARAM_PASSWORD = "passw0rd";
        String PARAM_QMGR_CHANNEL_NAME = "DEV.APP.SVRCONN";
        String PARAM_QMGR_NAME = "QM1";
        int PARAM_QMGR_PORT = 1414;
        String PARAM_QMGR_HOST_NAME = "localhost";

        Connection conn = null;

        try {

            // Set up the connection factory to connect to the queue manager,
            // populating it with all the properties we have been provided.
            MQConnectionFactory cf = new MQQueueConnectionFactory();
            cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            cf.setHostName(PARAM_QMGR_HOST_NAME);
            cf.setPort(PARAM_QMGR_PORT);
            cf.setQueueManager(PARAM_QMGR_NAME);
            cf.setChannel(PARAM_QMGR_CHANNEL_NAME);

            // Create the connection to the queue manager
            conn = cf.createConnection(PARAM_USERNAME, PARAM_PASSWORD);

            // Create a session and a queue object to enable us to interact with the
            // queue manager. By creating a transactional session we can roll back
            // the message onto the queue if the processing fails.
            Session session = conn.createSession(true, 0);
            Queue q = session.createQueue("queue:///"+PARAM_QUEUE_NAME);

            // For testing purposes we can put some messages onto the queue using this

            // Set up the consumer to read from the queue
            MessageConsumer consumer = session.createConsumer(q);

            // Don't forget to start the connection before trying to receive messages!
            conn.start();

            try {

                // Read messages from the queue and process them until there
                // are none left to read.
                while (true) {
                    Message receivedMsg = consumer.receiveNoWait();
                    if (receivedMsg != null) {
                        // use final -> multithreading env
                        final TextMessage rcvTxtMsg = (TextMessage) receivedMsg;
                        final String txt = rcvTxtMsg.getText();
                        final ProducerRecord<String, String> objectStringProducerRecord = new ProducerRecord<>(topic, null, txt);
                        kafkaProducer.send(objectStringProducerRecord, (recordMetadata, e) -> {
                           if (e!=null){
                               logger.warn("Producer call back end up with exception");
                               try {
                                   session.rollback();
                                   logger.warn("Ibm mq session rollback");
                               } catch (JMSException ex) {
                                   logger.warn("Ibm mq session failed to rollback");
                                   ex.printStackTrace();
                               }
                           } else {
                               try {
                                   session.commit();
                                   logger.info("Transaction success: Message was committed by IBM MQ Session and was delivered by Kafka-producer to kafka topic: {}, with offset: {}", recordMetadata.topic(), recordMetadata.offset());
                               } catch (JMSException ex) {
                                   logger.info("Transaction failed: Message was not committed by IBM MQ Session");
                                   throw new RuntimeException(e);
                               }
                           }
                        });


                        // Since we returned from processing the message without
                        // an exception being thrown we have successfully
                        // processed the message, so increment our success count
                        // and commit the transaction so that the message is
                        // permanently removed from the queue.
//                        messagesProcessed++;
//                        session.commit();
                    }

                }

            } catch (JMSException jmse2)
            {
                // This block catches any JMS exceptions that are thrown during
                // the business processing of the message.
                jmse2.printStackTrace();

                // Roll the transaction back so that the message can be put back
                // onto the queue ready to have its proessing retried next time
                // the action is invoked.
                session.rollback();
                throw new RuntimeException(jmse2);

            } catch (RuntimeException e) {
                e.printStackTrace();

                // Roll the transaction back so that the message can be put back
                // onto the queue ready to have its proessing retried next time
                // the action is invoked.
                session.rollback();
                throw e;
            }

            // Indicate to the caller how many messages were successfully processed.

        } catch (JMSException jmse) {
            // This block catches any JMS exceptions that are thrown before the
            // message is retrieved from the queue, so we don't need to worry
            // about rolling back the transaction.
            jmse.printStackTrace();

            // Pass an indication about the error back to the caller.
            throw new RuntimeException(jmse);

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException jmse2) {
                    // Swallow final errors.
                }
            }
        }

    }

    static Properties getKafkaProducerProperties() {
        final Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return properties;

    }


}