package de.fhdw.project;

public class ProjectApplication {

    public static void main(String[] args) {
        Queue queue = new Queue(5);

        System.out.println(queue);

        runSystem(3, 2, 2);
    }

    public static Queue runSystem(int capacity, int producerCount, int consumerCount) {
        Queue queue = new Queue(capacity);

        for (int i = 0; i < producerCount; i++) {
            String producerName = "p" + i;
            Producer xyz = new Producer(producerName, queue);
        }

        for (int i = 0; i < consumerCount; i++) {
            String consumerName = "c" + i;
            Consumer xyz = new Consumer(consumerName, queue);
        }

        //TODO Queue gedöns

        return queue;
    }

}
