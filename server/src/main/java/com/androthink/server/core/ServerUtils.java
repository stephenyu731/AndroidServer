package com.androthink.server.core;

import android.content.Context;
import android.util.Log;

import com.androthink.server.callback.ServerCallBack;
import com.androthink.server.model.Route;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;

public class ServerUtils implements ServerCallBack {

    private static ServerUtils serverUtils = null;
    private final static String TAG = "ServerUtils";

    private Thread streamerThread;
    private ServerRunnable streamer = null;

    private int port;
    private List<Route> routeList;
    private ServerCallBack callBack;

    private KeyManagerFactory keyManagerFactory;

    public static ServerUtils getInstance(List<Route> routeList,int port){
        if(serverUtils == null)
            serverUtils = new ServerUtils(routeList,port,null);

        return serverUtils;
    }

    public static ServerUtils getInstance(List<Route> routeList,int port,ServerCallBack callBack){
        if(serverUtils == null)
            serverUtils = new ServerUtils(routeList,port,callBack);

        return serverUtils;
    }

    private ServerUtils(){}

    private ServerUtils(List<Route> routeList,int port,ServerCallBack callBack) {
        this.routeList = routeList;
        this.callBack = callBack;
        this.port = port;
    }

    public void setSSL(KeyStore keystore, char[] keystorePassword) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        // 创建 KeyManagerFactory 使用密钥库
        keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, keystorePassword);
    }


    public void start(Context context) {

        if (streamerThread != null) {
            Log.e(TAG, "Server Already Started .");
            return;
        }

        try {
            if (keyManagerFactory != null) {
                streamer = new ServerRunnable(context, port, routeList, ServerUtils.this, keyManagerFactory);
            } else {
                streamer = new ServerRunnable(context, port, routeList, ServerUtils.this);
            }
            streamerThread = new Thread(streamer);

            streamerThread.start();
            Log.i(TAG, "Server Started .");

        } catch (IOException e) {
            serverUtils = null;
            e.printStackTrace();
            Log.e(TAG, "Exception while Starting Server !");
        }
    }

    public void stop(){
        if (streamer != null) {
            streamer.stop();

            streamer = null;
        }else {
            Log.e(TAG, "Server Already Stopped !");
        }
    }

    public boolean isRunning(){
        return streamerThread != null;
    }

    @Override
    public void onServerStopped() {
        if(streamerThread.isAlive()){
            streamerThread.interrupt();
        }

        streamerThread = null;

        if(callBack != null)
            callBack.onServerStopped();
        Log.e(TAG, "Server Stopped .");
    }
}