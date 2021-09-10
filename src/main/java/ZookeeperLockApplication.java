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
        lock.lock();
        System.out.println("线程A成功获取到锁");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(int i=0;i<100;i++) {
            holdCounter.count++;
        }
        lock.unlock();
//        Thread thread1=  new Thread(new Runnable() {
//            @Override
//            public void run() {
//                zookeeperLock.lock("dddd");
//                System.out.println("线程A成功获取到锁");
//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                for(int i=0;i<100;i++) {
//                    holdCounter.count++;
//                }
//                zookeeperLock.unlock("dddd");
//            }
//        });
//        Thread thread2=  new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//                zookeeperLock.lock("dddd");
//                System.out.println("线程B成功获取到锁");
//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                for(int i=0;i<100;i++) {
//                    holdCounter.count++;
//                }
//                zookeeperLock.unlock("dddd");
//            }
//        });
       // thread1.start();
       // thread2.start();
      //  thread1.join();
        //thread2.join();
        System.out.println(holdCounter.count);
    }
}
