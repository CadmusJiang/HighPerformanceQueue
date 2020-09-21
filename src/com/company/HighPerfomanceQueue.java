package com.company;

import com.company.exception.IllegalSizeException;
import sun.misc.Unsafe;

import java.util.HashMap;
import java.util.Map;

public class HighPerfomanceQueue<E> {
    private long frontOffset;
    private long rearOffset;
    private Unsafe help;
    private boolean[] available;
    private int max_size =16;
    private Object[] data;
    private long p1,p2,p3,p4,p5,p6,p7,p8;
    private long front;//队头一端，只允许删除
    private long rear;//队尾一端，只允许插入操作
    public HighPerfomanceQueue() throws NoSuchFieldException {
        this(1<<10);
        this.help = Util.createUnsafe();
        frontOffset = help.objectFieldOffset(HighPerfomanceQueue.class.getDeclaredField("front"));
        rearOffset = help.objectFieldOffset(HighPerfomanceQueue.class.getDeclaredField("rear"));
    }
    public HighPerfomanceQueue(int size){
        if(size <= 0) throw new IllegalArgumentException("size must be positive");
        if((size & (size-1)) != 0){
            throw new IllegalArgumentException("size must be multiple of 2");
        }
        this.max_size = size;
        front = rear = 0;
        data = new Object[max_size];
        available = new boolean[max_size];
    }
    //判断是否为空
    public boolean isEmpty(){
        return rear==front?true:false;
    }
    //入队
    public boolean add(E e){
        long expect = rear;
        int index= (int)((expect) & (max_size - 1));
        int cnt = 0;
        for(;;) {
            expect = rear;
            index= (int)((expect) & (max_size - 1));
            if (!available[index]) {
                HighPerfomanceQueue q = this;
                if (help.compareAndSwapLong(q, rearOffset, expect, expect + 1)) {
                    break;
                }
            }
            cnt++;
            //自旋100次退出
            if(cnt==100){
                return false;
            }
        }
        data[index] = e;
        available[index] = true;
        return true;
    }
//    //返回队首元素,不删除元素
//    public E peek(){
//        return (E) data[(int)((front) & (max_size - 1))];
//    }
    //出队
    public E poll(){
        int cnt = 0;
        long expect = front;
        int index = (int)(expect&(max_size-1));
        E e = (E) data[index];
        for(;;) {
            HighPerfomanceQueue q = this;
            expect = front;
            index = (int)(expect&(max_size-1));
            if(available[index]) {
                e = (E) data[index];
                if (help.compareAndSwapLong(q, frontOffset, expect, expect + 1)) {
                    available[index] = false;
                    break;
                }
            }
            cnt++;
            //自旋100次退出
            if(cnt==100){
                return null;
            }
        }
        return e;
    }
}



/*public class HighPerfomanceQueue<E> implements Queue<E> {
    private int size = 1>>24;// default, size must be multiple of 2
    private long p1,p2,p3,p4,p5,p6,p7;// solve Pseudo sharing;
    private volatile long allocateIndex;// need mutex
    private long p8,p9,p10,p11,p12,p13,p14;//solve Pseudo sharing
    private volatile long readCursor;
    private volatile long writeCursor;
    private volatile boolean isStart = false;// 开始后不能修改数组大小
    private volatile int[] queue;
    private Map<Integer,E> map = new HashMap<>();
    HighPerformanceQueue(){
        readCursor = -1;
        writeCursor = -1;
    }
    boolean setSize(int size)throws Exception{
        if(isStart)return false;
        if(size <= 0) throw new IllegalSizeException("size must be positive");
        if((size &= (size-1)) != 0){
            throw new IllegalSizeException("size must be multiple of 2");
        }
        this.size = size;
        queue = new E[size];
        return true;
    }

    @Override
    public boolean add(E[] e) {
        // from now, size can't be modify
        isStart = true;
//        long tmp  = writeCursor - readCursor+ e.length;
//        if(tmp<size)return false;
//        if()

    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E element() {
        return null;
    }
}*/
