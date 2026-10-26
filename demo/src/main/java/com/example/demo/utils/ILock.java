package com.example.demo.utils;

public interface ILock {

    boolean tryGetLock(long timeOut);

    void unlock();
}
