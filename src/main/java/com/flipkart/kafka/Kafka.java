package com.flipkart.kafka;


import java.util.*;
import java.util.concurrent.ExecutionException;

import javafx.animation.PauseTransition;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.internals.Topic;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;


public class Kafka {
    private Properties producer_props;
    private Properties consumer_props;

    public Kafka(){
        producer_props = new Properties();
        consumer_props = new Properties();

    }

    public KafkaProducer createProducer(){
        producer_props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producer_props.put(ProducerConfig.ACKS_CONFIG, "all");
        producer_props.put(ProducerConfig.RETRIES_CONFIG, 0);
        producer_props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer_props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        producer_props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:5001");

        return new KafkaProducer(producer_props);
    }

    public void insert_word_into_kafka(KafkaProducer producer, String topic){
        Scanner sc = new Scanner(System.in);
        String line;
        for (int i = 0;  i < 1; i++){
            line = sc.nextLine();
            final ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, i+"", line);
            try{
                RecordMetadata metadata = (RecordMetadata) producer.send(record).get();
            }
            catch (ExecutionException e) {
                System.out.println("Error in sending record");
                System.out.println(e);
            }
            catch (InterruptedException e) {
                System.out.println("Error in sending record");
                System.out.println(e);
            }
            System.out.println("Produce Record Successfully");
        }
    }

    public void insert_into_kafka(KafkaProducer producer, String topic){
        for (long i = 2000; i < 2010; i++) {
            final String orderId = "id" + Long.toString(i);
            final Payment payment = new Payment(orderId, "RICKY", 1000.00d, "Manager ", 100);
            final ProducerRecord<String, Payment> record = new ProducerRecord<String, Payment>(topic, payment.getId().toString(), payment);
            try {
                RecordMetadata metadata = (RecordMetadata) producer.send(record).get();
                System.out.println("Record sent with key " + orderId + " to partition " + metadata.partition()
                        + " with offset " + metadata.offset()+ " to topic " + metadata.topic());
            }
            catch (ExecutionException e) {
                System.out.println("Error in sending record");
                System.out.println(e);
            }
            catch (InterruptedException e) {
                System.out.println("Error in sending record");
                System.out.println(e);
            }
            System.out.println("Produce Record Successfully");
        }
    }

    public KafkaConsumer createConsumer(){
        consumer_props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumer_props.put(ConsumerConfig.GROUP_ID_CONFIG, "java-consumer-app");
        consumer_props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumer_props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer_props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:5001");
        consumer_props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumer_props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumer_props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        return new KafkaConsumer(consumer_props);
    }

    public void read_from_kafka(KafkaConsumer consumer, String topic){
        //consumer.subscribe(Collections.singletonList(topic));
        Collection ar = new ArrayList();
        TopicPartition tp_1 = new TopicPartition(topic, 0);
        TopicPartition tp_2 = new TopicPartition(topic, 1);
        TopicPartition tp_3 = new TopicPartition(topic, 2);
        ar.add(tp_1);
        ar.add(tp_2);
        ar.add(tp_3);
        consumer.assign(ar);
        consumer.seekToBeginning(ar);

        while (true) {
            System.out.println("FETCHING RECORD FROM KAFKA");
            ConsumerRecords<String, Payment> records = consumer.poll(1000L);
            System.out.println(records.isEmpty());
            for (ConsumerRecord<String, Payment> record : records) {
                String key = record.key();
                System.out.println("  key => "+ key + "  value => " + record.value() +"  topic =>  "+record.topic() + "    partition  =>  "+record.partition());
            }
            consumer.commitSync();
            break;
        }
    }
}
