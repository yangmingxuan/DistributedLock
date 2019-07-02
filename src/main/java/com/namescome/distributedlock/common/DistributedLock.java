package com.namescome.distributedlock.common;

public interface DistributedLock {

    public boolean tryLock(String LockKey, String LockValue);
    public boolean getLock(String LockKey, String LockValue);
    public void waitLock (String LockKey);
    public boolean unLock(String LockKey, String LockValue);

}
