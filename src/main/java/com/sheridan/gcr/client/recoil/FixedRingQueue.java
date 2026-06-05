package com.sheridan.gcr.client.recoil;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

public final class FixedRingQueue<T> implements Iterable<T> {
    private static final int CAPACITY = 13;
    private final Object[] buffer = new Object[CAPACITY];
    private int head = 0; // 指向最旧元素
    private int tail = 0; // 指向下一个写入位置
    private int size = 0;

    /** 入队（满时覆盖最旧元素） */
    public void offer(T value) {
        buffer[tail] = value;
        tail++;
        if (tail == CAPACITY) tail = 0;

        if (size < CAPACITY) {
            size++;
        } else {
            head++;
            if (head == CAPACITY) head = 0;
        }
    }

    /** 出队 */
    @SuppressWarnings("unchecked")
    public T poll() {
        if (size == 0)
            throw new IllegalStateException("Queue empty");

        T value = (T) buffer[head];
        head++;
        if (head == CAPACITY) head = 0;
        size--;
        return value;
    }

    /** 查看队首元素 */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (size == 0)
            throw new IllegalStateException("Queue empty");
        return (T) buffer[head];
    }

    /** 按插入顺序遍历（不移除元素） */
    @SuppressWarnings("unchecked")
    public void forEachOrdered(Consumer<? super T> action) {
        int index = head;
        for (int i = 0; i < size; i++) {
            action.accept((T) buffer[index]);
            index++;
            if (index == CAPACITY) index = 0;
        }
    }

    /** 当前元素数量 */
    public int size() {
        return size;
    }

    /** 是否为空 */
    public boolean isEmpty() {
        return size == 0;
    }

    /** 是否已满 */
    public boolean isFull() {
        return size == CAPACITY;
    }

    /** 清空 */
    public void clear() {
        head = tail = size = 0;
    }

    /** 按插入顺序的 Iterator（for-each 支持） */
    @Override
    public @NotNull Iterator<T> iterator() {
        return new Iterator<>() {
            private int index = head;
            private int count = 0;

            @Override
            public boolean hasNext() {
                return count < size;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T next() {
                T value = (T) buffer[index];
                index++;
                if (index == CAPACITY) index = 0;
                count++;
                return value;
            }
        };
    }
}
