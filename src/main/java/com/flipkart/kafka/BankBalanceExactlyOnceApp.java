package com.flipkart.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Instant;
import java.util.Properties;

public class BankBalanceExactlyOnceApp {
    public static void main(String[] args) {
        Properties config = new Properties();

        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "bank-balance-application");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // we disable the cache to demonstrate all the "steps" involved in the transformation - not recommended in prod
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");

        // Exactly once processing!!
        config.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);

        // json Serde
        final Serializer<JsonNode> jsonSerializer = new JsonSerializer();
        final Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
        final Serde<JsonNode> jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);


        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, JsonNode> bankTransactions =
                builder.stream("bank-transactions", Consumed.with(Serdes.String(), jsonSerde));


        // create the initial json object for balances
        ObjectNode initialBalance = JsonNodeFactory.instance.objectNode();
        initialBalance.put("id", 0);
        initialBalance.put("balance", 0);
        initialBalance.put("time", 0);

        KTable<String, JsonNode> bankBalance = bankTransactions.groupByKey(Serialized.with(Serdes.String(), jsonSerde))
                .aggregate(
                        () -> {
                            return initialBalance;
                        },
                        (key, transaction, balance) -> {
                            System.out.println("NOW, I AM HERE  " + key + "  ->  "+transaction.toString()+"  ->  "+balance.toString());
                            return newBalance(transaction, balance);
                        },
                        Materialized.with(Serdes.String(), jsonSerde)
                );

        //bankBalance.to(Serdes.String(), jsonSerde,"bank-balance-exactly-once");
        bankBalance.toStream().peek((key, value) -> System.out.println(key + "  FINALLY FUCK  " + value.toString()));
        KafkaStreams streams = new KafkaStreams(builder.build(), config);

        streams.cleanUp();
        streams.start();

        // print the topology
        System.out.println(streams.toString());

        // shutdown hook to correctly close the streams application
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static JsonNode newBalance(JsonNode transaction, JsonNode balance) {
        // create a new balance json object
        ObjectNode newBalance = JsonNodeFactory.instance.objectNode();
        newBalance.put("id", balance.get("id"));
        if (balance.get("time").asInt() < transaction.get("time").asInt()){
            newBalance.put("balance", balance.get("balance").asInt() + transaction.get("amount").asInt());
            newBalance.put("time", transaction.get("time").asInt());
        }
        else{
            newBalance.put("balance", balance.get("balance").asInt());
            newBalance.put("time", balance.get("time").asInt());
        }
        return newBalance;
    }
}
