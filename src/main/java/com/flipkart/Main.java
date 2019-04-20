package com.flipkart;

import com.flipkart.kafka.*;
import com.flipkart.storm.bolts.SplitSentenceBolt;
import com.flipkart.storm.bolts.WordCountBolt;
import com.flipkart.storm.spouts.SentenceSpout;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.apache.log4j.Logger;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;

import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.transactional.TransactionalTopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.tuple.Values;
import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.*;

public class Main {
    private static final String TOPIC = "sentence-stream";

    public static void kafka_producer_consumer(){
        final Logger logger = Logger.getLogger(Main.class);
        Kafka kafka = new Kafka();
        KafkaProducer producer = kafka.createProducer();
        kafka.insert_word_into_kafka(producer, TOPIC);
        KafkaConsumer consumer = kafka.createConsumer();
        kafka.read_from_kafka(consumer, TOPIC);
    }

//    public static void kafka_stream(){
//        KafkaStream kafkaStream = new KafkaStream();
//        StreamsBuilder streamsBuilder = kafkaStream.create_kafka_stream("wordcount-application");
//        kafkaStream.start_word_count_stream(streamsBuilder, "input", "output");
//    }
//
//    public static void read_from_rock_db(){
//        RocksDB.loadLibrary();
//        try (final Options options = new Options().setCreateIfMissing(false)) {
//            try (final RocksDB db = RocksDB.openReadOnly(options, "/tmp/kafka-streams/bank-application/1_1/rocksdb/KSTREAM-AGGREGATE-STATE-STORE-0000000003")) {
//                System.out.println(db.getSnapshot().getSequenceNumber());
//                try (final RocksIterator iterator = db.newIterator()) {
//                    for (iterator.seekToLast(); iterator.isValid(); iterator.prev()) {
//                        byte[] b_1 = iterator.key();
//                        byte[] b_2 = iterator.value();
//
//                        String s_1 = "";
//                        String s_2 = "";
//                        for (int i = 0; i < b_1.length; i++)
//                            s_1 += ((char)b_1[i] + "");
//
//                        System.out.println(Arrays.toString(b_2));
//
//                        System.out.println(s_1 + "      kl      " + s_2);
//                    }
//                }
//                System.out.println("NOTHING");
//            }
//        } catch (RocksDBException e) {
//            System.out.println("UNABLE TO CONNECT");
//        }
//    }

    public static void main(String[] args) throws InvalidTopologyException, AuthorizationException, AlreadyAliveException {
        //kafka_producer_consumer();
        //FavouriteColor favouriteColor = new FavouriteColor();

        //read_from_rock_db();
        //BankBalance bankBalance = new BankBalance();
        //bankBalance.produce_records();
        //bankBalance.consume_record();
        //bankBalance.start_transaction();

        /* NATIVE STORM TOPOLOGY */
//        SentenceSpout spout = new SentenceSpout();
        SplitSentenceBolt splitBolt = new SplitSentenceBolt();
        WordCountBolt wordCountBolt = new WordCountBolt();
//        TopologyBuilder topologyBuilder = new TopologyBuilder();
//        topologyBuilder.setSpout("sentence-spout", spout, 2);
//        topologyBuilder.setBolt("split-bolt", splitBolt, 4).shuffleGrouping("sentence-spout");
//        topologyBuilder.setBolt("count-bolt", wordCountBolt, 4).fieldsGrouping("split-bolt", new Fields("TupleValue"));
//
//        Config config = new Config();
//        config.setNumWorkers(2);
//        StormSubmitter.submitTopology("WordCountTopology", config, topologyBuilder.createTopology());

        /* NATIVE STORM WITH KAFKA SPOUT */

        Fields fields = new Fields("topic", "offset", "key", "message", "partition");
        TopologyBuilder topologyBuilder = new TopologyBuilder();

        KafkaSpoutConfig kafkaSpoutConfig = KafkaSpoutConfig.builder("localhost:9092", TOPIC)
                .setGroupId("wordcount-application")
                .setTupleTrackingEnforced(true)
                .setOffsetCommitPeriodMs(100_000)
                .setFirstPollOffsetStrategy(KafkaSpoutConfig.FirstPollOffsetStrategy.EARLIEST)
                .setRecordTranslator((r) -> new Values(r.topic(),  r.offset(), r.key(), r.value(), r.partition()), new Fields("topic", "offset", "key", "message", "partition"))
                .build();

        topologyBuilder.setSpout("sentence-spout", new KafkaSpout(kafkaSpoutConfig), 3);
        topologyBuilder.setBolt("split-bolt", splitBolt, 4).shuffleGrouping("sentence-spout");
        topologyBuilder.setBolt("count-bolt", wordCountBolt, 4).fieldsGrouping("split-bolt", new Fields("message"));

        Config config = new Config();
        config.setNumWorkers(3);
        config.setDebug(true);
        StormSubmitter.submitTopology("WordCountTopology", config, topologyBuilder.createTopology());

        /* TRIDENT TOPOLOGY */

    }
}