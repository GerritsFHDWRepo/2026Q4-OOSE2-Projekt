package de.fhdw.project;

import java.util.ArrayList;
import java.util.List;

public class Consumer extends Thread {

    private final String name;
    private final Queue queue;
    private final List<Integer> history = new ArrayList<>();

    public Consumer(String name, Queue queue){
        this.name = name;
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < Producer.N; i++) {
                history.add( this.queue.get());
            }
        } catch (InterruptedException e) {
            System.out.println("Consumer thread interrupted");
        }
    }

    public Integer[] getConsumed() {
        return this.history.toArray(new Integer[0]);
    }

}
