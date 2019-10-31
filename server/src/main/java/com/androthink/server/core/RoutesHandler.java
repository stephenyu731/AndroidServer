package com.androthink.server.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.androthink.server.handler.ResponseHandler;
import com.androthink.server.helper.ServerHelper;
import com.androthink.server.model.Request;
import com.androthink.server.model.Route;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RoutesHandler {

    private static RoutesHandler routesHandler = null;

    private List<Route> routeList;

    static RoutesHandler getInstance(){
        if(routesHandler == null)
            routesHandler = new RoutesHandler();

        return routesHandler;
    }

    private RoutesHandler(){
        this.routeList = new ArrayList<>();
    }

    void Handle(Request request, ResponseHandler responseHandler)throws IOException{

        if(!isAllowedMethod(request.getMethod()))
            responseHandler.sendJsonResponse(ServerHelper.RESPONSE_CODE.METHOD_NOT_ALLOWED,
                    "{\"status\":false,\"error\":\"Method (" + request.getMethod() + ") Not Allowed!\"}");

        Route route = getRoute(request.getMethod(),request.getRoutePath());

        if(route != null) {
            if (route.isAuth()) {
                String authKey = getAuthorizationKey(request.getHeaders());
                if(authKey != null){
                    request.setApiKey(authKey);
                    route.getCallBack().onRequested(request,responseHandler);
                }else{
                    if (isApiRoute(request.getRoutePath()))
                        responseHandler.sendJsonResponse(ServerHelper.RESPONSE_CODE.UNAUTHORIZED,
                                "{\"status\":false,\"error\":\"Error 401 UnAuthorized !\"}");
                    else
                        responseHandler.sendHtmlFileResponse(ServerHelper.RESPONSE_CODE.UNAUTHORIZED, "html/login.html");
                }
            } else
                route.getCallBack().onRequested(request,responseHandler);
        }else {
            if(request.getMethod().equals(ServerHelper.METHOD.GET)){
                if(request.getRoutePath().startsWith("/images/")){
                    responseHandler.sendImageResponse(ServerHelper
                            .getFileFromAssets(responseHandler.getContext(),
                                    request.getRoutePath().replace("/images/", "img/")));

                    return;
                }else if(request.getRoutePath().startsWith("/sounds/")){
                    responseHandler.sendImageResponse(ServerHelper
                            .getFileFromAssets(responseHandler.getContext(),
                                    request.getRoutePath().replace("/sounds/", "sound/")));

                    return;
                }
            }

            if (isApiRoute(request.getRoutePath()))
                responseHandler.sendJsonResponse(ServerHelper.RESPONSE_CODE.NOT_FOUND,
                        "{\"status\":false,\"error\":\"Error 404 Not Found !\"}");
            else {
                Map<String, String> placeHolders = new HashMap<>();
                placeHolders.put("page_place_holder", "" + request.getRoutePath());
                responseHandler.sendHtmlFileResponse(ServerHelper.RESPONSE_CODE.NOT_FOUND, "html/404.html", placeHolders);
            }
        }
    }

    void applyRoutes(List<Route> routeList){
        this.routeList = routeList;
    }

    @Nullable
    private Route getRoute(String method,String path){

        for (Route route:routeList) {
            if(route.getMethod().equals(method) && route.getPath().equals(path))
                return route;
        }

        return null;
    }

    private boolean isAllowedMethod(@NonNull String method){
        switch (method) {
            case ServerHelper.METHOD.GET:
            case ServerHelper.METHOD.POST:
            case ServerHelper.METHOD.PUT:
            case ServerHelper.METHOD.DELETE:
                return true;
            default:
                return false;
        }
    }

    private boolean isApiRoute(@NonNull String path){
        return path.startsWith("/api/v" + ServerHelper.API_VERSION);
    }

    @Nullable
    private String getAuthorizationKey(@NonNull Map<String,String> headers){
        for(String key : headers.keySet()){
            if(key.equals(ServerHelper.MAIN_HEADERS.AUTHORIZATION))
                return headers.get(key);
        }

        return null;
    }
}