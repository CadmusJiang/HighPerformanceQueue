package com.company;

import java.util.Collection;
import java.util.NoSuchElementException;

public interface Queue<E>{
    boolean add(E[] e);

    E poll();

    E element();
}
