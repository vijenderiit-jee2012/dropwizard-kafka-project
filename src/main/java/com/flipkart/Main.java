package com.flipkart;

import com.flipkart.kafka.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.apache.log4j.Logger;

import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Timer;

public class Main {
    private static final String TOPIC = "transactions";

    public static void kafka_producer_consumer(){
        final Logger logger = Logger.getLogger(Main.class);
        Kafka kafka = new Kafka();
        KafkaProducer producer = kafka.createProducer();
        kafka.insert_into_kafka(producer, TOPIC);
        KafkaConsumer consumer = kafka.createConsumer();
        kafka.read_from_kafka(consumer, TOPIC);
    }

    public static void kafka_stream(){
        KafkaStream kafkaStream = new KafkaStream();
        StreamsBuilder streamsBuilder = kafkaStream.create_kafka_stream("wordcount-application");
        kafkaStream.start_word_count_stream(streamsBuilder, "input", "output");
    }

    public static void read_from_rock_db(){
        RocksDB.loadLibrary();
        try (final Options options = new Options().setCreateIfMissing(false)) {
            try (final RocksDB db = RocksDB.openReadOnly(options, "/tmp/kafka-streams/bank-application/1_1/rocksdb/KSTREAM-AGGREGATE-STATE-STORE-0000000003")) {
                System.out.println(db.getSnapshot().getSequenceNumber());
                try (final RocksIterator iterator = db.newIterator()) {
                    for (iterator.seekToLast(); iterator.isValid(); iterator.prev()) {
                        byte[] b_1 = iterator.key();
                        byte[] b_2 = iterator.value();

                        String s_1 = "";
                        String s_2 = "";
                        for (int i = 0; i < b_1.length; i++)
                            s_1 += ((char)b_1[i] + "");

                        System.out.println(Arrays.toString(b_2));

                        System.out.println(s_1 + "      kl      " + s_2);
                    }
                }
                System.out.println("NOTHING");
            }
        } catch (RocksDBException e) {
            System.out.println("UNABLE TO CONNECT");
        }
    }

    public static void main(String[] args){
        //kafka_producer_consumer();
        //FavouriteColor favouriteColor = new FavouriteColor();
        read_from_rock_db();
        //BankBalance bankBalance = new BankBalance();
        //bankBalance.produce_records();
        //bankBalance.consume_record();
//        try{
//            Thread.sleep(100L);
//        }catch(InterruptedException e){System.out.println(e);}
     //   bankBalance.start_transaction();
        //bankBalance.getRockDBData(bankBalance.streams).stream().peek(key -> System.out.println(key));

    }


}

