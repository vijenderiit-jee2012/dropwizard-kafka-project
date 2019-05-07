package com.flipkart.storm.trident;

import org.apache.storm.trident.operation.BaseFunction;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Values;

public class Split extends BaseFunction {
    @Override
    public void execute(TridentTuple tridentTuple, TridentCollector tridentCollector) {
        String sentence = tridentTuple.getStringByField("message");
        for (String word : sentence.split(" ")){
            tridentCollector.emit(new Values(word));
        }
    }
}
