package de.fhdw.project;

import java.util.ArrayList;
import java.util.List;

/**
 * Dies ist ein Konsument, der Elemente aus unserer eigenen {@link Queue} entnehmen kann.
 */
public class Consumer extends Thread {

    private final Queue queue;
    private final List<Integer> history = new ArrayList<>();
    private final int actionAmount;

    public Consumer(String name, Queue queue, int actionAmount){
        super(name);
        this.queue = queue;
        this.actionAmount = actionAmount;
    }

    /**
     * Führt den Consumer mit der gewünschten Anzahl der Aktionen aus.
     * Dabei fügt er diese Aktionen auch in seine Historie ein.
     */
    @Override
    public void run() {
        try {
            for (int i = 0; i < actionAmount; i++) {
                int value = this.queue.get();

                synchronized (history) {
                    history.add(value);
                }

                System.out.println(getName() + " konsumiert: " + value);
            }
        } catch (InterruptedException e) {
            System.out.println("Consumer thread interrupted");
        }
    }

    /**
     * Funktion zum Ausgeben der Historie dieses Consumers.
     *
     * @return Historie als Integer Array
     */
    public Integer[] getConsumed() {
        synchronized (history) {
            return this.history.toArray(new Integer[0]);
        }
    }

}
