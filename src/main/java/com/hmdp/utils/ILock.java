package com.hmdp.utils;

public interface ILock {
    /**
     * try to get lock
     * @param timeoutSec the expiration time of the lock
     * @return true means successfully get; false means failure
     */
    boolean tryLock(long timeoutSec);

    /**
     * release lock
     */
    void unlock();
}
