package com.company;


import java.text.DecimalFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static long getStartTime() {
        return startTime;
    }

    public static void setStartTime(long startTime) {
        Main.startTime = startTime;
    }
    private static long startTime = 0;
    public static void main(String[] args) throws Exception {
        int writeThreadNums = 1;
        int readThreadNums = 1;
        long writeOperationNums = (int)1e7;
        int singleOperationNums = 100; // every write operations, add how much element into queue;
        long readOperationNums = writeOperationNums*writeThreadNums*singleOperationNums/readThreadNums;
        HighPerformanceQueue queue = new HighPerformanceQueue<String>(1<<15);
        AtomicLong cnt = new AtomicLong();// count number of out queue elements
        CountDownLatch latch = new CountDownLatch(writeThreadNums+readThreadNums);// End the queue when both the write and read processes are finished
        Integer[] tmp = new Integer[singleOperationNums];
        for(int i=0;i<singleOperationNums;i++){
            tmp[i] = new Integer(1);
        }
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
                    for(long j=0;j<writeOperationNums;j++){
                        while(!queue.add(tmp));
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
                    for(long j=0;j<readOperationNums;j++) {
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
        System.out.println("总数量:"+cnt.get()+" 总用时:"+time);
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        System.out.println("每秒吞吐量"+decimalFormat.format(cnt.get()*1.0/time));
    }
}