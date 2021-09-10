package lock;

import com.liqiang.comon.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.zookeeper.CreateMode;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.LockSupport;

/**
 * @Project redis-in-action
 * @PackageName com.lock
 * @ClassName ZookeeperLock
 * @Author qiang.li
 * @Date 2021/9/9 1:36 下午
 * @Description TODO
 */
public class ZookeeperLock {
    private static final String SYN_SWITCH_ZK_NODE = "/sync-lock";
    private ConcurrentHashMap<String, Lock> concurrentHashMap = new ConcurrentHashMap<>();

    public ZookeeperLock() throws Exception {
        if (CuratorClient.getCurator().checkExists().forPath(SYN_SWITCH_ZK_NODE)==null) {
            CuratorClient.getCurator()
                    .create()
                    //因为一般情况开发人员在创建一个子节点必须判断它的父节点是否存在，如果不存在直接创建会抛出NoNodeException，
                    // 使用creatingParentContainersIfNeeded()之后Curator能够自动递归创建所有所需的父节点。
                    .creatingParentContainersIfNeeded()
                    //创建永久节点
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(SYN_SWITCH_ZK_NODE);
        }

    }


    public  Lock get(String key) {
        synchronized (key.intern()) {
            Lock sync = concurrentHashMap.get(key);
            if (sync == null) {
                sync = new Lock(new Sync(key));
                concurrentHashMap.put(key, sync);
            }
            return sync;
        }
    }
    public static class Lock{
        Sync sync=null;
        public Lock(Sync sync){
            this.sync=sync;
        }
        public void lock() {
            sync.acquire(1);
        }
        public void unlock() {
            sync.release(1);
        }
    }

    static class Sync extends AbstractQueuedSynchronizer {
        private String key;

        private SyncValue syncValue=new SyncValue();

        private CuratorFramework curatorFramework;

        public Sync(String key) {
            this.key = key;
            this.curatorFramework = CuratorClient.getCurator();
        }

        @Override
        protected boolean tryAcquire(int acquires) {
            //获取当前线程
            final Thread current = Thread.currentThread();
            //获取锁状态
            int c = getState();
            //等于0表示 当前空闲状态可以尝试获取
            if (c == 0) {
                if (zkLock()&&compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            //可重入判断
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            //状态-1 大于0的数字表示可重入加了多少次锁
            int c = getState() - releases;
            //如果加锁线程非当前线程抛出异常
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            String value;
            try {
                value= new String(curatorFramework.getData().forPath(getLockPath()));
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalMonitorStateException();
            }
            if(value==null||!value.equals(syncValue.get().getValue())){
                throw new IllegalMonitorStateException();
            }
            boolean free = false;
            //当c等于0表示最后一次调用unlock 则进行锁的释放
            if (c == 0) {
                free = true;
                //获得锁的线程设置为null
                setExclusiveOwnerThread(null);
                String path= getLockPath();
                try {
                    curatorFramework.delete().forPath(path);
                } catch (Exception e) {
                    throw new IllegalMonitorStateException();
                }
                syncValue.remove();
            }
            //设置state
            setState(c);

            return free;
        }

        public boolean zkLock() {
            String path= getLockPath();
            boolean haveLock=false;
            try {
                curatorFramework
                        .create()
                        .creatingParentContainersIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(path, syncValue.get().getValue().getBytes(StandardCharsets.UTF_8));

                haveLock= true;
            } catch (org.apache.zookeeper.KeeperException.NodeExistsException e) {
                haveLock=false;
            } catch (Exception e) {
                e.printStackTrace();
                haveLock= false;
            }
            if(!haveLock){
                TreeCache treeCache = new TreeCache(CuratorClient.getCurator(), SYN_SWITCH_ZK_NODE);
                try {
                    treeCache.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(!syncValue.get().isAddListener) {
                    treeCache.getListenable().addListener(new TreeCacheListener() {
                        @Override
                        public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                            ChildData eventData = event.getData();
                            switch (event.getType()) {
                                case NODE_ADDED:
                                    System.out.println(path + "节点添加:" + eventData.getPath() + "\t添加数据为：" + new String(eventData.getData()));
                                    break;
                                case NODE_UPDATED:
                                    System.out.println(eventData.getPath() + "节点数据更新\t更新数据为：" + new String(eventData.getData()) + "\t版本为：" + eventData.getStat().getVersion());
                                    break;
                                case NODE_REMOVED:
                                    Thread firstThread=getFirstQueuedThread();
                                    if( firstThread!=null) {
                                        LockSupport.unpark(firstThread);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                    syncValue.get().setAddListener(true);
                }
            }
            return haveLock;
        }

        public String getLockPath(){
            String path = SYN_SWITCH_ZK_NODE + "/"+key;
            return path;
        }

        public static  class HoldCounter{
            private String value= UUID.randomUUID().toString().replace("-","");
            private boolean isAddListener=false;

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            public boolean isAddListener() {
                return isAddListener;
            }

            public void setAddListener(boolean addListener) {
                isAddListener = addListener;
            }
        }
        public static class SyncValue extends ThreadLocal<HoldCounter>{
            @Override
            protected HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

    }
}
