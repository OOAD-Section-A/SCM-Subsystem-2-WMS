package wms.services;

import java.util.LinkedList;
import java.util.Queue;
import wms.commands.IWarehouseTask;

/**
 * Description: The Invoker in the Command Pattern.
 * Queues and processes tasks for warehouse workers.
 */
public class TaskEngine {
    private Queue<IWarehouseTask> taskQueue;

    public TaskEngine() {
        this.taskQueue = new LinkedList<>();
    }

    public void scheduleTask(IWarehouseTask task) {
        taskQueue.add(task);
        System.out.println("TaskEngine: New task added to the worker queue. (Queue size: " + taskQueue.size() + ")");
    }

    public void executeAllPendingTasks() {
        System.out.println("\nTaskEngine: Processing all pending worker tasks...");
        while (!taskQueue.isEmpty()) {
            IWarehouseTask task = taskQueue.poll();
            task.execute();
        }
        System.out.println("TaskEngine: All tasks completed. Queue is empty.");
    }
}
