package de.fhdw.project;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Wrapper für eine thread-sichere FIFO-Warteschlange für Integer-Werte mit fester Kapazität.
 * Das Einfügen blockiert bei voller, das Entnehmen bei leerer Warteschlange.
 *
 * <p>Die Implementierung verwendet eine {@link LinkedBlockingQueue}.
 */
public class Queue {

    private final BlockingQueue<Integer> queue;
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
        Integer peeked = this.queue.peek();

        if(peeked == null) {
            throw new NullPointerException("Peek kann nicht auf eine leere Queue angewandt werden.");
        }

        return peeked;
    }

    /**
     * Dies ist die maximale Größe der Queue.
     *
     * @return Kapazität als int
     */
    public int capacity() {
        return this.capacity;
    }

    /**
     * Dies ist die aktuelle Anzahl der in der Queue enthaltenen Elemente.
     *
     * @return Größe als int
     */
    public int size() {
        return this.queue.size();
    }

    /**
     * Konvertiert die Queue für eine bessere Ausgabe zu einem Array in der FIFO-Reihenfolge.
     * @return Queue als Array
     */
    public Integer[] toArray() {
        return this.queue.toArray(new Integer[0]);
    }

}
