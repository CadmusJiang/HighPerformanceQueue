package com.company;

import sun.misc.Unsafe;

import java.util.Random;

public class HighPerformanceQueue<E> implements Queue<E> {
    private Unsafe help;
    private long frontOffset;
    private long rearOffset;
    private Random random = new Random();
    private int max_size =1<<14;// Ring array maximum
    private Object[] data; // Ring array
    private boolean[] available; // Help read determine whether the current object has been written

    // adaptive spin, The effect is not good, close
    // add
    private int addMaxTryNum = (int)1e3; // Maximum number of measurements
    private double addMaxTryNumProbability = 0.01; //Increase measurement probability,must <= 1
    private int addTryNum = addMaxTryNum; // Adaptive attempts
    private int addTryOffset = (int)1e2; // To avoid monotonic decreasing, correct the value
    // poll
    private int pollMaxTryNum = (int)1e2; // Maximum number of measurements
    private double pollMaxTryNumProbability = 0.01; //Increase measurement probability,must <= 1
    private int pollTryNum = pollMaxTryNum; // Adaptive attempts
    private int pollTryOffset = (int)1e2; // To avoid monotonic decreasing, correct the value

    private int tryNum = (int)1e2;
    private long p1,p2,p3,p4,p5,p6,p7,p8;
    private volatile long front;// head ptr of queue;
    private volatile long rear;// tail ptr of queue;
    public HighPerformanceQueue(int size) throws NoSuchFieldException {
        if(size <= 0) throw new IllegalArgumentException("size must be positive");
        if((size & (size-1)) != 0){
            throw new IllegalArgumentException("size must be multiple of 2");
        }

        this.max_size = size;
        front = rear = 0;

        // Space pre allocation
        data = new Object[max_size];
        available = new boolean[max_size];

        // Get the offset in the object and modify it with CAS later
        this.help = Util.createUnsafe();
        frontOffset = help.objectFieldOffset(HighPerformanceQueue.class.getDeclaredField("front"));
        rearOffset = help.objectFieldOffset(HighPerformanceQueue.class.getDeclaredField("rear"));
    }
    // IsEmpty return is empty of queue
    public boolean isEmpty(){
        return rear==front?true:false;
    }

    // Add add element into queue
    public boolean add(E[] es){
        long expect = rear;
        int cnt = 0;

        // TODO Generate random number, affect performance, close
        // Increase the number of retries periodically to avoid offset
        // if( random.nextInt(maxTryNum)>maxTryNum*maxTryNumProbability){
        // tryNum = maxTryNum;
        // }

        int curTryNum = tryNum;
        for(;;) {
            expect = rear;
            boolean check = false;
            if (expect-front+es.length<=max_size) {
                HighPerformanceQueue q = this;
                if (help.compareAndSwapLong(q, rearOffset, expect, expect + es.length)) {
                    // TODO Dynamic maintenance retries
                    break;
                }
            }
            cnt++;
            if(cnt==tryNum){
                return false;
            }
        }
        for(int i=0;i<es.length;i++){
            int index = (int)((expect+i)&(max_size-1));
            data[index] = es[i];
            available[index] = true;
        }
        return true;
    }
    // Peek return head of element
    public E peek(){
        return front!=rear?(E) data[(int)((front) & (max_size - 1))]:null;
    }
    // Poll poll element from queue;
    public E poll(){
        int cnt = 0;
        long expect = front;
        int index;
        int curTryNum = tryNum;
        E e ;
        for(;;) {
            HighPerformanceQueue q = this;
            expect = front;
            index = (int)(expect&(max_size-1));
            if(available[index]) {
                e = (E) data[index];
                //TODO Additional write burden is added to the read operation
                available[index] = false;
                if (help.compareAndSwapLong(q, frontOffset, expect, expect + 1)) {
                    // TODO Dynamic maintenance retries
                    break;
                }
                available[index] = true;
            }
            cnt++;
            if(cnt==tryNum){
                return null;
            }
        }
        return e;
    }
}