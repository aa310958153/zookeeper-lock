import lock.ZookeeperLock;

import java.util.concurrent.locks.Lock;

/**
 * @Project zookeeper-lock
 * @PackageName PACKAGE_NAME
 * @ClassName ZookeeperLockApplication
 * @Author qiang.li
 * @Date 2021/9/9 3:46 下午
 * @Description TODO
 */
public class ZookeeperLockApplication {
    public static class  HoldCounter{
        int count=0;
    }
    public static void main(String[] args) throws Exception {
        ZookeeperLock zookeeperLock=new ZookeeperLock();
        HoldCounter holdCounter=new HoldCounter();
        ZookeeperLock. Lock lock=zookeeperLock.get("dddd");



        Thread thread1=  new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                System.out.println("线程A成功获取到锁");

                for(int i=0;i<100;i++) {
                    holdCounter.count++;
                }
                lock.unlock();
            }
        });
        Thread thread2=  new Thread(new Runnable() {
            @Override
            public void run() {

                lock.lock();

                for(int i=0;i<100;i++) {
                    holdCounter.count++;
                }
                lock.unlock();
            }
        });
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        System.out.println(holdCounter.count);
    }
}
