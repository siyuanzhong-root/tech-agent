package com.edu.agent.repair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心处理逻辑
 */
public class TaskDispatcher {

    // 按类型分组存放工作人员
    private final Map<TaskType, List<Worker>> workerMap = new HashMap<>();

    public TaskDispatcher() {
        initWorkers();
    }

    /**
     * 初始化人员：S:A:B:C = 1:2:2:1
     */
    private void initWorkers() {
        // S类 1人
        workerMap.put(TaskType.S, Collections.singletonList(new Worker("S-01", TaskType.S)));

        // A类 2人
        List<Worker> aWorkers = new ArrayList<>();
        aWorkers.add(new Worker("A-01", TaskType.A));
        aWorkers.add(new Worker("A-02", TaskType.A));
        workerMap.put(TaskType.A, aWorkers);

        // B类 2人
        List<Worker> bWorkers = new ArrayList<>();
        bWorkers.add(new Worker("B-01", TaskType.B));
        bWorkers.add(new Worker("B-02", TaskType.B));
        workerMap.put(TaskType.B, bWorkers);

        // C类 1人
        workerMap.put(TaskType.C, Collections.singletonList(new Worker("C-01", TaskType.C)));
    }

    /**
     * 分配任务
     */
    public Worker assignTask(TaskType dataType) {
        List<Worker> workers = workerMap.get(dataType);
        if (workers == null || workers.isEmpty()) {
            throw new RuntimeException("无对应类型处理人员：" + dataType);
        }

        // 筛选空闲人员
        List<Worker> idleWorkers = workers.stream()
                .filter(Worker::isIdle)
                .collect(Collectors.toList());

        if (idleWorkers.isEmpty()) {
            System.out.println("【警告】" + dataType + " 类人员全部忙碌，任务需等待");
            return null;
        }

        // 选第一个空闲的
        Worker target = idleWorkers.get(0);
        target.assignTask();
        System.out.println("【分配成功】数据类型 " + dataType + " → 处理人：" + target.getId());
        return target;
    }

    /**
     * 完成任务，释放人员
     */
    public void finishTask(Worker worker) {
        if (worker != null) {
            worker.finishTask();
            System.out.println("【任务完成】人员 " + worker.getId() + " 已空闲");
        }
    }

    /**
     * 打印所有人员状态
     */
    public void printAllStatus() {
        System.out.println("\n===== 当前所有人员状态 =====");
        workerMap.values().stream()
                .flatMap(List::stream)
                .forEach(w ->
                        System.out.println("人员：" + w.getId() + "  类型：" + w.getType() + "  状态：" + w.getStatus())
                );
        System.out.println("=============================\n");
    }
}