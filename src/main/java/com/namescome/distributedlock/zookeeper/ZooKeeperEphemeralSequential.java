package com.namescome.distributedlock.zookeeper;

import com.namescome.distributedlock.common.DistributedLock;

public class ZooKeeperEphemeralSequential implements DistributedLock {

    public ZooKeeperEphemeralSequential() {
        // TODO Auto-generated constructor stub
    }

    public boolean tryLock(String LockKey, String LockValue) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean getLock(String LockKey, String LockValue) {
        // TODO Auto-generated method stub
        return false;
    }

    public void waitLock(String LockKey) {
        // TODO Auto-generated method stub

    }

    public boolean unLock(String LockKey, String LockValue) {
        // TODO Auto-generated method stub
        return false;
    }

}
