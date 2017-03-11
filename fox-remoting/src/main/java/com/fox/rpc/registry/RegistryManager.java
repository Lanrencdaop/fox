package com.fox.rpc.registry;import com.fox.rpc.common.extension.UserServiceLoader;import com.fox.rpc.config.ConfigManager;import com.fox.rpc.config.ConfigManagerLoader;import com.fox.rpc.remoting.common.ServerInfo;import com.sun.xml.internal.bind.v2.model.core.ID;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import java.util.ArrayList;import java.util.List;import java.util.Properties;import java.util.concurrent.ConcurrentHashMap;/** * Created by shenwenbo on 2016/10/27. */public class RegistryManager {    private static Logger LOGGER= LoggerFactory.getLogger(RegistryManager.class);    private static RegistryManager instance = new RegistryManager();    private static ConfigManager configManager = ConfigManagerLoader.getConfigManager();    private static RegistryConfigManager registryConfigManager = new DefaultRegistryConfigManager();    private static volatile boolean isInit = false;    private static ConcurrentHashMap<String, ServerInfo> referencedAddresses = new ConcurrentHashMap<String, ServerInfo>();    private volatile static List<Registry> registryList=new ArrayList<>();    private static ConcurrentHashMap<String, String> registeredServices = new ConcurrentHashMap<String, String>();    public static RegistryManager getInstance() {        if (!isInit) {            synchronized (RegistryManager.class) {                if (!isInit) {                    instance.init(registryConfigManager.getRegistryConfig());                    RegistryEventListener.addListener(new InnerServerInfoListener());                    isInit = true;                }            }        }        return instance;    }    public static void init(Properties properties) {        List<Registry> _registryList = UserServiceLoader.getExtensionList(Registry.class);        if (_registryList.size() > 0) {            for (Registry registry : _registryList) {                registry.init(properties);                registryList.add(registry);            }        }    }    /***     * 获取服务，先从本地获取，本地获取不到时去注册中心获取；     * @param serviceName     * @return     */    public String getServiceAddress(String serviceName) {        String serviceAdress = registeredServices.get(serviceName);        if (serviceAdress!=null)            return serviceAdress;            for (Registry registry : registryList) {                serviceAdress=registry.getServiceAddress(serviceName);                registeredServices.putIfAbsent(serviceName, serviceAdress);                return serviceAdress;            }        return null;    }    public void registerService(String serviceName,String serviceAddress) {        for (Registry registry : registryList) {            registry.registerService(serviceName, serviceAddress);        }        registeredServices.putIfAbsent(serviceName, serviceAddress);        LOGGER.info("service register:"+serviceAddress);    }    /**监控事件处理*/    static class InnerServerInfoListener implements ServerInfoListener{        @Override        public void onServerVersionChange(String serverAddress, String version) {            ServerInfo serverInfo=referencedAddresses.get(serverAddress);            if (serverInfo!=null) {                serverInfo.setVersion(version);            }        }    }}