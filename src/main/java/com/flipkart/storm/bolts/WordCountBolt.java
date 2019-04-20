package com.flipkart.storm.bolts;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;

import java.util.HashMap;
import java.util.Map;

public class WordCountBolt extends BaseRichBolt {
    private OutputCollector collector;
    private String componentId;
    private HashMap<String, Integer> hashMap;
    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
        componentId = topologyContext.getThisComponentId() + ":WordCountBolt:" + topologyContext.getThisTaskId();
        hashMap = new HashMap<String, Integer>();
    }

    @Override
    public void execute(Tuple tuple) {
        String word = tuple.getStringByField("message");
        String key  = tuple.getStringByField("key");
        String topic = tuple.getStringByField("topic");
        int partition = tuple.getIntegerByField("partition");
        long offset = tuple.getLongByField("offset");

        //String UpstreamID  = tuple.getStringByField("SplitBoltID");
        //String BoltID      = UpstreamID + "=>" + this.componentId;
        if (hashMap.containsKey(word) == false)
            hashMap.put(word, 1);
        else
            hashMap.put(word, hashMap.get(word) + 1);
        System.out.println("CountBolt  => "+ word + " -> " + hashMap.get(word)  + " -> " + key + " -> " + topic + " -> " + partition + " -> " + offset);
        this.collector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }
}
