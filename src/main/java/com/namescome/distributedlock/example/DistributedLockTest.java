package com.namescome.distributedlock.example;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import com.namescome.distributedlock.common.DistributedLock;
import com.namescome.distributedlock.redis.RedisLock;
import com.namescome.distributedlock.zookeeper.ZooKeeperLock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class DistributedLockTest {

    private final static int TOTALNUM = 100;
    private int counter = 0;
    private CountDownLatch cdl = new CountDownLatch(TOTALNUM);

    private static String LockKey = "MYTESTLOCK";

    public JedisPool pool;
    
    public DistributedLockTest() {
        // TODO Auto-generated constructor stub
    }


    public static void main(String argv[]) {
        DistributedLockTest mytest = new DistributedLockTest();
        
        //No Lock example
        //mytest.doNoLockTest();

        //Redis Lock example
        //mytest.doRedisLockTest();

        //ZooKeeper Lock example
        mytest.doZkLockTest();
    }


    public void doNoLockTest() {
        for(int i = 0; i < TOTALNUM; i++) {
            new Thread(new NoLockTest()).start();
        }
    }

    class NoLockTest implements Runnable {

        public void run() {
            cdl.countDown();
            try {
                cdl.await();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            counter++;
            System.out.println(counter);
        }
        
    }

    public void doRedisLockTest() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(10);
        config.setMaxTotal(TOTALNUM+10);
        pool = new JedisPool(config, "172.31.138.138", 6379, 60000);
        for(int i = 0; i < TOTALNUM; i++) {
            new Thread(new RedisLockTest()).start();
        }

        while(counter < TOTALNUM) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        pool.close();
        pool.destroy();
    }

    class RedisLockTest implements Runnable {
        private RedisLock dbLock;
        
        RedisLockTest() {
            long lockDuration = 30000;   //The lock's expired time
            long eachWait = 50;      //Each milliseconds for waiting lock
            long maxWait = 300000;       //The max milliseconds for waiting lock
            dbLock = new RedisLock();
            dbLock.setLockDuration(lockDuration);
            dbLock.setEachWait(eachWait);
            dbLock.setMaxWait(maxWait);
            Jedis jedis = pool.getResource();
            dbLock.setJedis(jedis);
        }
 
        public void run() {
            String LockValue = UUID.randomUUID().toString() + "_" + Thread.currentThread().getName();
            cdl.countDown();
            try {
                cdl.await();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(dbLock.tryLock(LockKey, LockValue)) {
                counter++;
                System.out.println(counter);
            }
            dbLock.unLock(LockKey, LockValue);
            dbLock.getJedis().close();
        }
        
    }

    public void doZkLockTest() {
        for(int i = 0; i < TOTALNUM; i++) {
            new Thread(new ZkLockTest()).start();
        }
    }

    class ZkLockTest implements Runnable {
        private ZooKeeperLock dbLock;
        private CountDownLatch connectedSuc = new CountDownLatch(1);

        ZkLockTest () {
            long maxWait = 300000;       //The max milliseconds for waiting lock
            dbLock = new ZooKeeperLock();
            dbLock.setMaxWait(maxWait);
        }
 
        public void run() {
            String LockValue = UUID.randomUUID().toString() + "_" + Thread.currentThread().getName();
            cdl.countDown();
            try {
                cdl.await();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            int lockDuration = 100;
            String connectString = "172.31.138.138:2181";
            ZooKeeper zooKeeper = null;
            try {
                zooKeeper = new ZooKeeper(connectString, lockDuration, new Watcher() {

                    public void process(WatchedEvent event) {
                        Event.KeeperState state = event.getState();
                        Event.EventType type = event.getType();
                        if(state == Event.KeeperState.SyncConnected) { //if connecting
                            if(type == Event.EventType.None) { //if connected successfully
                                connectedSuc.countDown();
                                //System.out.println("Zookeeper connected successfully");
                            }
                        }
                    }
                    
                });
                try {
                    connectedSuc.await();  //wait to connected successfully
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } //
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(zooKeeper == null) {
                System.err.println("Can't Connect the ZooKeeper Server.");
                return;
            }
            dbLock.setZooKeeper(zooKeeper);
            String strLockKey = "/" + LockKey;
            if(dbLock.tryLock(strLockKey, LockValue)) {
                counter++;
                System.out.println(counter);
            }
            dbLock.unLock(strLockKey, LockValue);
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
