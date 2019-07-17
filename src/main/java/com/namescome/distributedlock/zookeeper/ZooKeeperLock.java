package com.namescome.distributedlock.zookeeper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.namescome.distributedlock.common.DistributedLock;

public class ZooKeeperLock implements DistributedLock {

    private CountDownLatch countDownLatch;

    private ZooKeeper zooKeeper;
    private long eachWait = 50;      //Each milliseconds for waiting lock
    private long maxWait = 30000;       //The max milliseconds for waiting lock
    private long startTime;    //Start time for waiting
    
    /**
     * @return the zooKeeper
     */
    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    /**
     * @param zooKeeper the zooKeeper to set
     */
    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    /**
     * @return the eachWait
     */
    public long getEachWait() {
        return eachWait;
    }

    /**
     * @param eachWait the eachWait to set
     */
    public void setEachWait(long eachWait) {
        this.eachWait = eachWait;
    }

   /**
     * @return the maxWait
     */
    public long getMaxWait() {
        return maxWait;
    }

    /**
     * @param maxWait the maxWait to set
     */
    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    public ZooKeeperLock() {
        // TODO Auto-generated constructor stub
    }

    public boolean tryLock(String LockKey, String LockValue) {
        startTime = System.currentTimeMillis();
        boolean isGetLock = false;
        do {
            isGetLock = getLock(LockKey, LockValue);
            if(!isGetLock) {
                waitLock(LockKey);
            }
        } while(!isGetLock && startTime + maxWait > System.currentTimeMillis());
        
        return isGetLock;
    }
    
    public boolean getLock(String LockKey, String LockValue) {
        try {
            zooKeeper.create('/' + LockKey, LockValue.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            return true;
        } catch (KeeperException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            return false;
        }
    }

    public void waitLock(String LockKey){
        Stat stat = null;
        try {
            stat = zooKeeper.exists('/' + LockKey, new Watcher() {
                public void  process(WatchedEvent event) {
                    if(countDownLatch != null) {
                        countDownLatch.countDown();
                    }
                }
            });
        } catch (KeeperException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //if the LockKey is not exist
        if(stat == null) {
            return;
        }
        
        //wait for the change
        countDownLatch = new CountDownLatch(1);
        try {
            countDownLatch.await(eachWait, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        countDownLatch = null;
    }

    public boolean unLock(String LockKey, String LockValue) {
        try {
            zooKeeper.delete('/' + LockKey, -1);
            // zooKeeper.close();  // close by the call Hierarchy
            return true;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeeperException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }
}
