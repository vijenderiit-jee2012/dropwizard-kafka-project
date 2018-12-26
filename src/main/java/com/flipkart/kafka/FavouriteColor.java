package com.flipkart.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Pattern;

public class FavouriteColor extends KafkaStream{
    ArrayList<String> valid_color;
    private Serde<String> stringSerde;
    private Serde<Long> longSerde;
    private StreamsBuilder streamsBuilder;
    private String input_topic = "color_input";
    private String output_topic = "color_output";

    public FavouriteColor(){
        valid_color = new ArrayList<String>();
        valid_color.add("RED");
        valid_color.add("BLUE");
        valid_color.add("GREEN");

        stringSerde = Serdes.String();
        longSerde = Serdes.Long();

        streamsBuilder = create_kafka_stream("favourite-colour-application");
        fav_color();
    }

    public void fav_color(){
        System.out.println("CREATE A STREAM FROM INPUT TOPIC");
        final KStream<String, String> color_stream = streamsBuilder.stream(input_topic);

        // FILTER THE STREAM
        final Pattern pattern = Pattern.compile(",", Pattern.UNICODE_CHARACTER_CLASS);
        final KStream<String, String> filter_color_stream = color_stream
                .filter((key, value) -> value != null && value.contains(","))
                .selectKey((key, value) -> value.split(",")[0].toUpperCase())
                .mapValues(value -> value.split(",")[1].toUpperCase())
                .filter((key, value) -> valid_color.contains(value));

        filter_color_stream.to("temp_color_topic", Produced.with(stringSerde, stringSerde));

        final KTable<String, String> color_final_table = streamsBuilder.table("temp_color_topic");

        final KTable<String, Long> fav_colors =  color_final_table
                .groupBy((user, color) -> new KeyValue<>(color, color))
                .count();

        fav_colors.toStream().peek((key, value) -> System.out.println(key + " ->   " + value));
        fav_colors.toStream().to(output_topic, Produced.with(stringSerde, longSerde));

        final KafkaStreams streams = new KafkaStreams(streamsBuilder.build(), stream_properties);


        System.out.println("STARTING THE STREAM");
        streams.cleanUp();
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}