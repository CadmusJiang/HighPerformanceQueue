package com.company;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static long getStartTime() {
        return startTime;
    }

    public static void setStartTime(long startTime) {
        Main.startTime = startTime;
    }
    private static long startTime = 0;
    public static void main(String[] args) throws Exception {
        int writeThreadNums = 3;
        int readThreadNums = 3;
        int writeOperationNums = (int)10e5;
        HighPerfomanceQueue queue = new HighPerfomanceQueue<String>();
        AtomicInteger cnt = new AtomicInteger();
        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(writeThreadNums+readThreadNums);
        CyclicBarrier cyclicBarrier = new CyclicBarrier(writeThreadNums + readThreadNums, new Runnable() {
            @Override
            public void run() {
                setStartTime(System.currentTimeMillis());
                System.out.println("测试开始");
            }
        });

        Thread[] writeThreads = new Thread[writeThreadNums];
        for(int i=0;i<writeThreads.length;i++){
            writeThreads[i] = new Thread(){
                @Override
                public void run() {
                    try {
                        cyclicBarrier.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    for(int j=0;j<writeOperationNums;j++){
                        while(!queue.add(1));
                    }
                    latch.countDown();
                }
            };
            writeThreads[i].start();
        }

        Thread[] readThreads = new Thread[readThreadNums];
        for(int i=0;i<readThreads.length;i++){
            readThreads[i] = new Thread(){
                @Override
                public void run() {
                    try {
                        cyclicBarrier.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    for(int j=0;j<writeOperationNums*writeThreadNums/readThreadNums;j++) {
                        while(true) {
                            Integer e = (Integer) queue.poll();
                            if (e != null) {
                                cnt.incrementAndGet();
                                break;
                            }
                        }
                    }
                    latch.countDown();
                }
            };
            readThreads[i].start();
        }
        latch.await();
        double time = (System.currentTimeMillis()-startTime)/1000.0;
        System.out.println("总数量"+cnt.get());
        System.out.println("每秒吞吐量"+cnt.get()*1.0/time);
//        List<Integer> help = new ArrayList<>(result);
//        Collections.sort(help);
//        for(int i=0;i<result.size();i++){
//            if(help.get(i)!=result.get(i)){
//                System.out.println(i);
//                break;
//            }
//        }
        for(int i=0;i<result.size();i++){
            System.out.print(result.get(i)+" ");
            if(i%10==0&&i!=0){
                System.out.println();
            }
        }

    }
}