package com.flipkart.kafka;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.*;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

public class BankBalance extends KafkaStream{
    private StreamsBuilder streamsBuilder;
    private String input_topic;
    private String output_topic;
    private Properties producer_props;
    private Properties consumer_props;
    final Map<String, String> serdeConfig;
    final SpecificAvroSerializer<BankAccount> bankEventSerializer;
    final SpecificAvroDeserializer<BankAccount> bankEventDeserializer;
    final SpecificAvroSerde<BankAccount> specificBankAvroSerde;
    final SpecificAvroSerde<FinalAccount> specificAccountAvroSerde;
    Random random;
    final String ACCOUNT_STORE = "account_store";
    ObjectNode initialBalance;
    public KafkaStreams streams;

    public BankBalance(){
        input_topic = "bank_input";
        output_topic = "bank_output";
        producer_props = new Properties();
        consumer_props = new Properties();

        streamsBuilder = create_kafka_stream("bank-application");
        serdeConfig = Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:5001");
        bankEventSerializer = new SpecificAvroSerializer();
        bankEventSerializer.configure(serdeConfig, false); // false for Value
        bankEventDeserializer = new SpecificAvroDeserializer();
        bankEventDeserializer.configure(serdeConfig, false);

        specificBankAvroSerde = new SpecificAvroSerde();
        specificBankAvroSerde.configure(serdeConfig, false);     // false for Value
        specificAccountAvroSerde = new SpecificAvroSerde<>();
        specificAccountAvroSerde.configure(serdeConfig, false);     // false for Value

        random = new Random();

        initialBalance = JsonNodeFactory.instance.objectNode();
        initialBalance.put("id", 0);
        initialBalance.put("balance", 0);
        initialBalance.put("time", 0.0);
    }

    public void consume_record(){
        consumer_props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumer_props.put(ConsumerConfig.GROUP_ID_CONFIG, "bank-consumer-app");
        consumer_props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumer_props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final KafkaConsumer<String, BankAccount> bankConsumer = new KafkaConsumer(consumer_props, Serdes.String().deserializer(), bankEventDeserializer);

        Collection ar = new ArrayList();
        TopicPartition tp_1 = new TopicPartition(input_topic, 0);
        TopicPartition tp_2 = new TopicPartition(input_topic, 1);
        ar.add(tp_1);
        ar.add(tp_2);
        bankConsumer.assign(ar);
        //bankConsumer.seekToBeginning(ar);

        while (true) {
            System.out.println("FETCHING RECORD FROM KAFKA");
            ConsumerRecords<String, BankAccount> records = bankConsumer.poll(1000L);
            if (records.isEmpty() == true) break;
            System.out.println(records.isEmpty());
            for (ConsumerRecord<String, BankAccount> record : records) {
                String key = record.key();
                System.out.println("  key => "+ key + "  value => " + record.value().toString() +"  topic =>  "+record.topic() + "    partition  =>  "+record.partition());
            }
            bankConsumer.commitSync();
        }
    }

    public Collection<StreamsMetadata> getRockDBData(KafkaStreams kafkaStreams){
        return kafkaStreams.allMetadataForStore(ACCOUNT_STORE);
    }

    public void produce_records(){
        producer_props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producer_props.put(ProducerConfig.ACKS_CONFIG, "all");


        final KafkaProducer<String, BankAccount> bankProducer = new KafkaProducer(producer_props, Serdes.String().serializer(), bankEventSerializer);

        Scanner sc = new Scanner(System.in);
        final Pattern pattern = Pattern.compile(",", Pattern.UNICODE_CHARACTER_CLASS);
        BankAccount bankAccount;
        int i = 0;
        String[] names = {"Vijender", "Bhavesh", "Aditya"};
        while (true){
            //System.out.println("Please Enter Customer Details :  ");
            //String[] transaction = pattern.split(sc.nextLine().trim());
            //if (transaction[0].equals("false")){
            //    bankProducer.close();
            //    break;
            //}

            //int customer_id = Integer.parseInt(transaction[0]);
            //String customer_name = transaction[1];
            double customer_salary = random.nextInt(10000000) + 5;
            long timestamp = random.nextInt(1000000000) + 1000000000;
            int partition = random.nextInt(5) % 2;
            int id = random.nextInt(10) % names.length;

            bankAccount = new BankAccount(id + 1, names[id], customer_salary, timestamp);

            bankProducer.send(new ProducerRecord<String, BankAccount>(input_topic, partition, String.valueOf(id + 1), bankAccount));
             i += 1;
            if (i % 1000 == 0)
                System.out.println("Customer Salary Published");
             if (i == 1000000)
                 break;

        }
    }

    public void start_transaction(){
        System.out.println("CREATE A STREAM FROM INPUT TOPIC");
        final KStream<String, BankAccount> bank_stream = streamsBuilder.stream(input_topic,
                Consumed.with(Serdes.String(), specificBankAvroSerde));


        System.out.println("STARTING THE STREAM");
        bank_stream.peek((key, value) -> System.out.println(key + "     " + value.toString()));

        // Use groupBy , instead of groupByKey to avoid creation of internal topic;
        final KTable<String, FinalAccount> account_table = bank_stream.
                groupBy((key, value) -> key, Grouped.with(Serdes.String(), specificBankAvroSerde)).
                aggregate(
                        () -> {
                            System.out.println("I AM HERE");
                            return new FinalAccount(0, 0.0, 0L);
                        },
                        (key, bankAccount, finalAccount) -> {
                            finalAccount.setId(bankAccount.getId());
                            System.out.println("NOW, I AM HERE     "+ finalAccount.toString() + "  to    "+ bankAccount.toString());
                            if (bankAccount.getTimestamp() > finalAccount.getTimestamp()) {
                                finalAccount.setAmount(bankAccount.getAmount());
                                finalAccount.setTimestamp(bankAccount.getTimestamp());
                            }
                            return finalAccount;
                        },
                        Materialized.<String, FinalAccount, KeyValueStore<Bytes, byte[]>>as(ACCOUNT_STORE).with(Serdes.String(), specificAccountAvroSerde)
                );

        account_table.toStream().peek((key, value) -> System.out.println(key + " FUCK " + value.toString()));

        //account_table.toStream().to(output_topic);
        streams = new KafkaStreams(streamsBuilder.build(), stream_properties);

        streams.start();

        //streams.allMetadataForStore(ACCOUNT_STORE);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));


         // ReadOnlyKeyValueStore<String, FinalAccount> keyValueStore =
//                streams.store(ACCOUNT_STORE, QueryableStoreTypes.<String, FinalAccount>keyValueStore());
//
//        KeyValueIterator<String, FinalAccount> range = keyValueStore.range("all", "streams");
//        while (range.hasNext()) {
//            KeyValue<String, FinalAccount> next = range.next();
//            System.out.println("count for " + next.key + ": " + next.value.toString());
//        }
    }
}
