package com.fox.rpc.registry.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by shenwenbo on 2016/10/17.
 */
public class CuratorClient {

    private static Logger LOGGER= LoggerFactory.getLogger(CuratorClient.class);

    private String address;

    private static final String CHARSET = "UTF-8";

    private CuratorFramework zookeeperClient;

    public CuratorClient(String zkAddress) throws InterruptedException {
        this.address=zkAddress;
        newZkClient();
    }

    /**
     * 初始化zk客户端
     */
    public boolean newZkClient() throws InterruptedException {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .ensembleProvider(new RegionEnsembleProvider(address))
                .sessionTimeoutMs(30 * 1000)
                .connectionTimeoutMs( 15 * 1000)
                .retryPolicy(new ExponentialBackoffRetry(1000, Integer.MAX_VALUE))
                .build();

        client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                LOGGER.info("zookeeper state changed to " + newState);
                if (newState == ConnectionState.RECONNECTED) {
//                    RegistryEventListener.connectionReconnected();
                }
            }
        });
       // client.getCuratorListenable().addListener(new CuratorEventListener(this), curatorEventListenerThreadPool);
        client.start();
        boolean isConnected = client.getZookeeperClient().blockUntilConnectedOrTimedOut();
        CuratorFramework oldClient = this.zookeeperClient;
        this.zookeeperClient = client;
        close(oldClient);
        LOGGER.info("succeed to create zookeeper client, connected:" + isConnected);
        return isConnected;
    }

    public CuratorFramework getZookeeperClient() {
        return zookeeperClient;
    }

    /**
     * 获取根据path获取节点信息
     * @param path
     * @return
     */
    public List<String> get(String path) throws Exception {
        if (!exists(path)) {
            throw new RuntimeException(String.format("can not find any service node on path: %s", path));
        }
        List<String> addressList = null;
        try {
            addressList = zookeeperClient.getChildren().watched().forPath(path);
        } catch (Exception e) {
            LOGGER.debug("node " + path + " does not exist");
            return null;
        }
        return addressList;
    }

    /**
     * 创建零时节点
     * @param node
     * @param value
     */
    public void create(String node,String value) throws Exception {
        byte[] bytes = (value == null ? new byte[0] : value.toString().getBytes(CHARSET));
        String addressNode = zookeeperClient.create().withMode(CreateMode.EPHEMERAL).forPath(node,bytes);
        LOGGER.info("create address node:",addressNode);
    }

    /**
     * 创建持久节点
     * @param path
     */
    public void creatrPersistentNode(String path) throws Exception {
        if (!exists(path)) {
            zookeeperClient.create().creatingParentsIfNeeded().forPath(path);
            LOGGER.info("create registry node:",path);
        }
    }

    /**
     * 删除节点
     * @param path
     * @return
     */
    public void delete(String path) throws Exception {
        zookeeperClient.delete().forPath(path);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("delete node " + path);
        }
    }

    /***
     * 删除存在的path
     * @param path
     * @throws Exception
     */
    public void deleteIfExists(String path) throws Exception {
        if (exists(path, false)) {
            delete(path);
        } else {
            LOGGER.warn("node " + path + " not exists!");
        }
    }




    private void close(CuratorFramework client) {
        if (client != null) {
            LOGGER.info("begin to close zookeeper client");
            try {
                client.close();
                LOGGER.info("succeed to close zookeeper client");
            } catch (Exception e) {
            }
        }
    }

    public boolean exists(String path) throws Exception {
        Stat stat = zookeeperClient.checkExists().watched().forPath(path);
        return stat != null;
    }


    public boolean exists(String path, boolean watch) throws Exception {
        Stat stat = watch ? zookeeperClient.checkExists().watched().forPath(path) : zookeeperClient.checkExists().forPath(path);
        return stat != null;
    }

}
