package com.flipkart.storm.bolts;

import org.apache.storm.generated.Bolt;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.Map;

public class SplitSentenceBolt extends BaseRichBolt {
    private OutputCollector collector;
    private String componentId;

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
        this.componentId = topologyContext.getThisComponentId() + ":SplitBolt:" + topologyContext.getThisTaskId();
    }

    @Override
    public void execute(Tuple tuple) {
        String sentence = tuple.getStringByField("message");
        String key  = tuple.getStringByField("key");
        String topic = tuple.getStringByField("topic");
        int partition = tuple.getIntegerByField("partition");
        long offset = tuple.getLongByField("offset");

        //String UpstreamID  = tuple.getStringByField("SplitBoltID");
        //String BoltID      = UpstreamID + "=>" + this.componentId;
        for (String word :  sentence.split(" ")){
            word = word.toLowerCase();
            System.out.println("SplitBolt EMITTED => "+ word + " -> " + key + " -> " + topic + " -> " + sentence + " -> " + offset);
            this.collector.emit(tuple, new Values(key, word, topic, partition, offset));
        }
        this.collector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("key", "message", "topic", "partition", "offset"));
    }
}
