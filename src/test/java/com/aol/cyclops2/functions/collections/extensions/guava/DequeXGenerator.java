package com.aol.cyclops2.functions.collections.extensions.guava;

import java.util.Queue;

import cyclops.collections.DequeX;
import com.google.common.collect.testing.TestStringQueueGenerator;

public class DequeXGenerator extends  TestStringQueueGenerator {

    
    @Override
    public Queue<String> create(String... elements) {
       return DequeX.of(elements);
    }

}
