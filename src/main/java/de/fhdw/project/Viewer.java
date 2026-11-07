package de.fhdw.project;

public class Viewer extends Thread {

    private final Queue queue;

    public Viewer(Queue queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < Producer.N; i++) {
                this.queue.peek();
            }
        } catch (NullPointerException e) {
            System.out.println("Viewer cannot Peek at an empty queue");
        }
    }

}
