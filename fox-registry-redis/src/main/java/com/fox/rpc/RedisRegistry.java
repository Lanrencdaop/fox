package com.fox.rpc;

import com.fox.rpc.registry.Registry;
import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * Created by shenwenbo on 16/8/23.
 */
public class RedisRegistry implements Registry {

    private static Logger LOGGER = Logger.getLogger(RedisRegistry.class);

    @Override
    public void init(Properties properties) {

    }

    @Override
    public void registerService(String serviceName, String serviceAddress) {

    }

    @Override
    public void unregisterService(String serviceName, String serviceAddress) {
    }

    @Override
    public String getServiceAddress(String serviceName) {
        return null;
    }
}