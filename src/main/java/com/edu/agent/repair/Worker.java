package com.edu.agent.repair;

/**
 * 工人类型
 */
public class Worker {
    private final String id;
    private final TaskType type;
    private WorkerStatus status;

    public Worker(String id, TaskType type) {
        this.id = id;
        this.type = type;
        this.status = WorkerStatus.IDLE;
    }

    // 分配任务
    public void assignTask() {
        this.status = WorkerStatus.BUSY;
    }

    // 完成任务
    public void finishTask() {
        this.status = WorkerStatus.IDLE;
    }

    // 判断是否空闲
    public boolean isIdle() {
        return status == WorkerStatus.IDLE;
    }

    // getter
    public String getId() {
        return id;
    }

    public TaskType getType() {
        return type;
    }

    public WorkerStatus getStatus() {
        return status;
    }
}