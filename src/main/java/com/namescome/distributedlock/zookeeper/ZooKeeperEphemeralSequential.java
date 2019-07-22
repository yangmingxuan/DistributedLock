package com.namescome.distributedlock.zookeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

public class ZooKeeperEphemeralSequential implements DistributedLock {

    private CountDownLatch countDownLatch;

    private ZooKeeper zooKeeper;
    private long lockDuration = 30000;   //The lock's expired time
    private long eachWait = 50;      //Each milliseconds for waiting lock
    private long maxWait = 30000;       //The max milliseconds for waiting lock
    private long startTime;    //Start time for waiting

    private String SPACEPATH = "/MYDISTRIBUTEDLOCKPATH";
    private String currentPath = null;
    private String previousPath = "";

    /**
     * @return the zooKeeper
     */
    public ZooKeeper getConnection() {
        return zooKeeper;
    }

    /**
     * @param zooKeeper the zooKeeper to set
     */
    public void setConnection(Object connection) {
        this.zooKeeper = (ZooKeeper)connection;
    }

    /**
     * @return the lockDuration
     */
    public long getLockDuration() {
        return lockDuration;
    }



    /**
     * @param lockDuration the lockDuration to set
     */
    public void setLockDuration(long lockDuration) {
        this.lockDuration = lockDuration;
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

    public ZooKeeperEphemeralSequential() {
        // TODO Auto-generated constructor stub
    }

    private void initialSpacePath() {
        Stat stat = null;
        try {
            stat = zooKeeper.exists(SPACEPATH, false);
        } catch (KeeperException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
        if(stat != null) {
            return;
        }
        try {
            zooKeeper.create(SPACEPATH, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }

    public boolean tryLock(String LockKey, String LockValue) {
        initialSpacePath();
        startTime = System.currentTimeMillis();
        boolean isGetLock = false;
        do {
            isGetLock = getLock(LockKey, LockValue);
            if(!isGetLock) {
                waitLock(previousPath);
            }
        } while(!isGetLock && startTime + maxWait > System.currentTimeMillis());
        
        return isGetLock;
    }

    private boolean getLock(String LockKey, String LockValue) {
        try {
            if (currentPath == null || currentPath.length() <= 0) { //create one time
                currentPath = zooKeeper.create(SPACEPATH+ '/' + LockKey, LockValue.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            }
            List<String> allChildren = zooKeeper.getChildren(SPACEPATH, false);
            List<String> children = getLockChileren(allChildren, LockKey);
            if(children == null || children.isEmpty()) {
                previousPath = SPACEPATH + '/' + LockKey;
                return false;
            }
            Collections.sort(children);
            if (currentPath.equals(SPACEPATH + '/' + children.get(0))) {
                return true;
            } else {
                //find the previous node
                int id = Collections.binarySearch(children, currentPath.substring(SPACEPATH.length()+1));
                previousPath = SPACEPATH + '/' + children.get(id-1);
                return false;
            }
        } catch (KeeperException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            previousPath = SPACEPATH + '/' + LockKey;
            return false;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            previousPath = SPACEPATH + '/' + LockKey;
            return false;
        }
    }

    private void waitLock(String LockKey) {
        Stat stat = null;
        try {
            stat = zooKeeper.exists(LockKey, new Watcher() {
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
            zooKeeper.delete(currentPath, -1);
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

    private List<String> getLockChileren(List<String> allChildren, String LockKey) {
        if(allChildren == null) {
            return null;
        }
        List<String> children = new ArrayList<String>();
        for(String child: allChildren) {
            if(child.startsWith(LockKey)) {
                children.add(child);
            }
        }
        return children;
    }
}
