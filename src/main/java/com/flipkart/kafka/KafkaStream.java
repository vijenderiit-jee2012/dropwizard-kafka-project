package com.flipkart.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.kstream.ValueMapper;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class KafkaStream  {
    Properties stream_properties;
    private Serde<String> stringSerde;
    private Serde<Long> longSerde;

    public KafkaStream(){
        stream_properties = new Properties();
        stringSerde = Serdes.String();
        longSerde = Serdes.Long();
    }

    public StreamsBuilder create_kafka_stream(String application_id){
        stream_properties.put(StreamsConfig.APPLICATION_ID_CONFIG, application_id);
        stream_properties.put(StreamsConfig.CLIENT_ID_CONFIG, application_id);
        stream_properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        stream_properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        stream_properties.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
        stream_properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");
        stream_properties.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);


        StreamsBuilder streamsBuilder = new StreamsBuilder();
        return streamsBuilder;
    }

    public void start_word_count_stream(StreamsBuilder streamsBuilder, String input_topic, String output_topic){
        // Start a KStream from the input topic
        System.out.println("CREATE A STREAM FROM INPUT TOPIC");
        final KStream<String, String> textLines = streamsBuilder.stream(input_topic);

        // Create a topic with your logics
        Pattern pattern = Pattern.compile("\\W+", Pattern.UNICODE_CHARACTER_CLASS);

        System.out.println("PRINT THE BUILT STREAM");
        textLines.flatMapValues(new ValueMapper<String, Iterable<?>>() {
            @Override
            public Iterable<?> apply(String value) {
                System.out.println(value);
                return Arrays.asList(pattern.split(value.toLowerCase()));
            }
        });

        final KTable<String, Long> wordCounts = textLines.
                flatMapValues(value -> Arrays.asList(pattern.split(value.toLowerCase()))).
                selectKey((key, value) -> value).
                groupByKey().
                count();

        System.out.println("WRITTING TO OUTPUT KAFKA TOPIC " + output_topic);
        wordCounts.toStream().to(output_topic, Produced.with(stringSerde, longSerde));

        final KafkaStreams streams = new KafkaStreams(streamsBuilder.build(), stream_properties);

        System.out.println("STARTING THE STREAM");
        streams.cleanUp();
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
