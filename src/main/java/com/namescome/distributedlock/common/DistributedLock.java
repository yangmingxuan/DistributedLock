package com.namescome.distributedlock.common;

public interface DistributedLock {

    public void setLockDuration(long lockDuration);
    public long getLockDuration();

    public void setEachWait(long eachWait);

    public void setMaxWait(long maxWait);

    public void setConnection(Object connection);
    
    public boolean tryLock(String LockKey, String LockValue);

    public boolean unLock(String LockKey, String LockValue);
}
