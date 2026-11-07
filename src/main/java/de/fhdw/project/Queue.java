package de.fhdw.project;

import java.util.concurrent.LinkedBlockingQueue;

public class Queue {

    private final LinkedBlockingQueue<Integer> queue;
    private final int capacity;

    public Queue(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.capacity = capacity;
    }

    public int get() throws InterruptedException {
        return this.queue.take();
    }

    public void put(int value) throws InterruptedException {
        this.queue.put(value);
    }

    public int peek() {
        if(this.queue.isEmpty()) {
            throw new NullPointerException("Peek kann nicht auf eine leere Queue angewandt werden.");
        }
        return this.queue.peek();
    }

    public int capacity() {
        return this.capacity;
    }

    public int size() {
        return this.queue.size();
    }

    public Integer[] toArrayList() {
        return this.queue.toArray(new Integer[0]);
    }

}
