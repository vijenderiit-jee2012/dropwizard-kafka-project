package com.flipkart.storm.spouts;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SentenceSpout extends BaseRichSpout {
    private SpoutOutputCollector collector;
    private int id;
    private String componentId = "0";
    private String[] sentences = {
            "I am the BeSt",
            "i Support CSK",
            "We are happy if csk wins",
            "Ashwin and Dhoni are good captains",
            "Watson and Ponting are legends"
    };

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this.collector = spoutOutputCollector;
        this.componentId = topologyContext.getThisComponentId() + ":Spout:" + topologyContext.getThisTaskId();
        this.id = 0;
    }

    @Override
    public void ack(Object var1){
        System.out.println("PROCESSED => "+var1.toString());
    }

    @Override
    public void fail(Object var1){
        System.out.println("FAILED => "+var1.toString());
    }

    @Override
    public void nextTuple() {
        Random random = new Random();
        int index = Math.abs(random.nextInt()) % this.sentences.length;
        System.out.println("EMITTED => "+this.sentences[index] + " -> " + this.id + " -> " + this.componentId);
        this.collector.emit(new Values(this.id, this.sentences[index], this.componentId), this.componentId);
        this.id += 1;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("TupleID", "TupleValue", "SpoutID"));
    }
}