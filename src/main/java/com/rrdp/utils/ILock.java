package com.rrdp.utils;

public interface ILock {

    boolean tryLock(Long timeOut);

    void unclock();
}
