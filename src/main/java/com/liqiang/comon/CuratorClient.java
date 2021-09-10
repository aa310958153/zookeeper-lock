package com.liqiang.comon;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @Project redis-in-action
 * @PackageName com.lock
 * @ClassName CuratorClient
 * @Author qiang.li
 * @Date 2021/9/9 1:45 下午
 * @Description zookeeper客户端
 */
public class CuratorClient {
    private static CuratorFramework curator = null;
    static {
            String zookeeperHosts="192.168.20.4:2181";
            curator = CuratorFrameworkFactory.builder()
                    .connectString(zookeeperHosts)
                    .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                    .sessionTimeoutMs(6000)
                    .connectionTimeoutMs(3000)
                    .namespace("zk-lock")
                    .build();
            curator.start();
        }

    public static CuratorFramework getCurator() {
        return curator;
    }
}



