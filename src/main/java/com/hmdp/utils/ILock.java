package com.hmdp.utils;

public interface ILock {

    boolean tryLock(Long timeOut);

    void unclock();
}
