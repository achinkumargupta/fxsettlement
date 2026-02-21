package com.example.app;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
//export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
// cd java-project/

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public final class KafkaProducerExample {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(KafkaProducerExample.class);
        logger.info("Current working directory: " + System.getProperty("user.dir"));
        String TOPIC_NAME = "truck_locations";
        String TRUSTSTORE_PASSWORD = "Learning@wells";
        String KEYSTORE_PASSWORD = "Learning@wells";
        String KEY_PASSWORD = "Learning@wells";
        
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-39fa367e-achinai-a995.b.aivencloud.com:25670");
        //properties.setProperty("bootstrap.servers", "kafka-39fa367e-achinai-a995.b.aivencloud.com:25670");
        properties.setProperty("security.protocol", "SSL");
        properties.setProperty("ssl.truststore.location", "src/main/resources/certs/client.truststore.jks");
        properties.setProperty("ssl.truststore.password", TRUSTSTORE_PASSWORD);
        properties.setProperty("ssl.keystore.type", "PKCS12");
        properties.setProperty("ssl.keystore.location", "src/main/resources/certs/client.keystore.p12");
        properties.setProperty("ssl.keystore.password", KEYSTORE_PASSWORD);
        properties.setProperty("ssl.key.password", KEY_PASSWORD);
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        // create a producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // produce 100 messages
        for (int j=0; j<2;j++) {
            for (int i = 0; i < 10; i++) {
                String message = "Hello from Java using  " + (i + 1) + "!";
                producer.send(new ProducerRecord<String, String>(TOPIC_NAME, "key" + i, message),
                    new Callback() {
                        @Override
                        public void onCompletion(RecordMetadata metadata, Exception exception) {
                            if (exception != null) {
                                logger.error("Error sending message", exception);
                                return;
                            }
                            else {
                                logger.info("Message sent to topic: " + metadata.topic() + " partition: " + metadata.partition() + " offset: " + metadata.offset());
                            }
                        }
                    });
            }
        }
        producer.flush();
        producer.close();

   

        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        properties.setProperty("group.id", "groupid"+String.valueOf(System.currentTimeMillis()));
        properties.setProperty("auto.offset.reset", "earliest");

        // create a consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(Arrays.asList(TOPIC_NAME));
        while (true) {
            logger.info("Polling for messages...");
            ConsumerRecords<String, String> messages = consumer.poll(Duration.ofMillis(100));
            messages.forEach(message -> {
              System.out.println("Got message using SSL: " + message.value());
            });
        }
    }
}