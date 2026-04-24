package com.edu.agent.repair;

public class Main {
    public static void main(String[] args) {
        TaskDispatcher dispatcher = new TaskDispatcher();

        // 初始状态
        dispatcher.printAllStatus();

        // 分配一批任务
        System.out.println("=== 开始分配任务 ===");
        Worker w1 = dispatcher.assignTask(TaskType.S);
        Worker w2 = dispatcher.assignTask(TaskType.A);
        Worker w3 = dispatcher.assignTask(TaskType.A);
        Worker w4 = dispatcher.assignTask(TaskType.B);
        Worker w5 = dispatcher.assignTask(TaskType.C);

        // 此时A、B都满了
        System.out.println("\n=== 再次分配A类、B类 ===");
        dispatcher.assignTask(TaskType.A);
        dispatcher.assignTask(TaskType.B);

        dispatcher.printAllStatus();

        // 完成一些任务
        System.out.println("=== 完成部分任务 ===");
        dispatcher.finishTask(w2);
        dispatcher.finishTask(w4);

        dispatcher.printAllStatus();

        // 再次分配
        System.out.println("=== 再次分配 ===");
        dispatcher.assignTask(TaskType.A);
        dispatcher.assignTask(TaskType.B);
    }
}