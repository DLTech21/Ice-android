package io.github.dltech21.ice_android;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import Ice.ObjectPrx;

/**
 * Ice封装
 */
public class IceClientUtil {
    private static volatile Ice.Communicator ic = null;
    @SuppressWarnings("rawtypes")
    private static Map<Class, ObjectPrx> cls2PrxMap = new HashMap<Class, ObjectPrx>();
    private static Context context;

    /**
     * @return Ice.Communicator
     */
    public static Ice.Communicator getIceCommunicator() {
        if (ic == null) {
            synchronized (IceClientUtil.class) {
                if (ic == null) {
                    Ice.InitializationData initData = new Ice.InitializationData();
                    initData.properties = Ice.Util.createProperties();

                    try {
                        InputStream inputStream = context.getResources().getAssets().open("iceclient.properties");
                        Properties properties = new Properties();
                        properties.load(inputStream);
                        for (String name : properties.stringPropertyNames()) {
                            String value = properties.getProperty(name);
                            initData.properties.setProperty(name, value);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    ic = Ice.Util.initialize(initData);
                }
            }
        }
        return ic;
    }

    public static void closeCommunicator(boolean removeServiceCache) {
        synchronized (IceClientUtil.class) {
            if (ic != null) {
                safeShutdown();
                if (removeServiceCache && !cls2PrxMap.isEmpty()) {
                    try {
                        cls2PrxMap.clear();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void safeShutdown() {
        try {
            ic.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ic.destroy();
            ic = null;
        }
    }

    /**
     * 用发射方式创建Object Proxy
     *
     * @param communicator
     * @param serviceCls
     * @return ObjectPrx
     */
    @SuppressWarnings("rawtypes")
    private static ObjectPrx createIceProxy(Ice.Communicator communicator, Class serviceCls) {
        ObjectPrx proxy = null;
        String clsName = serviceCls.getName();
        String serviceName = serviceCls.getSimpleName();
        int pos = serviceName.lastIndexOf("Prx");
        if (pos <= 0) {
            throw new IllegalArgumentException("Invalid ObjectPrx class ,class name must end with Prx");
        }
        String realSvName = serviceName.substring(0, pos);
        try {
            ObjectPrx base = communicator.stringToProxy(realSvName);
            proxy = (ObjectPrx) Class.forName(clsName + "Helper").newInstance();
            Method m1 = proxy.getClass().getDeclaredMethod("checkedCast", ObjectPrx.class);
            proxy = (ObjectPrx) m1.invoke(proxy, base);
            return proxy;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 用于客户端API获取Ice服务实例的场景
     *
     * @param serviceCls
     * @return ObjectPrx
     */
    @SuppressWarnings("rawtypes")
    public static ObjectPrx getServicePrx(Context context, Class serviceCls) {
        IceClientUtil.context = context;
        ObjectPrx proxy = cls2PrxMap.get(serviceCls);
        if (proxy != null) {
            return proxy;
        }
        proxy = createIceProxy(getIceCommunicator(), serviceCls);
        cls2PrxMap.put(serviceCls, proxy);
        return proxy;
    }

}
