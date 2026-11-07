package de.fhdw.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Producer extends Thread {

    public static final int N = 5;

    private final String name;
    private final Queue queue;
    private final List<Integer> history = new ArrayList<>();

    public Producer(String name, Queue queue){
        this.name = name;
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < N; i++) {
                Random random = new Random();
                int inserted = random.nextInt();
                this.queue.put(inserted);
                history.add(inserted);
            }
        } catch (InterruptedException e) {
            System.out.println("Producer thread interrupted");
        }
    }

    public Integer[] getProduced() {
        return this.history.toArray(new Integer[0]);
    }

}
