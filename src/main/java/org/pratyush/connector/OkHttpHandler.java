/*
 * Copyright © 2022 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.pratyush.connector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class OkHttpHandler {
    private static final OkHttpClient okHttpClient;
    private static final Gson gson;

    static {
        okHttpClient = new OkHttpClient();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();


    }

    private RESTConnectorConfig connectorConfig;

    public OkHttpHandler(RESTConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
    }

    public static Gson getGsonObj(){
        return gson;
    }

    public Request generateRequest() {
        StringBuilder baseUrl = new StringBuilder(connectorConfig.getBaseURL());

        if (!baseUrl.toString().endsWith("/"))
            baseUrl.append("/");
        Request.Builder requestBuilder = new Request.Builder()
                .url(baseUrl + connectorConfig.getEndPoint());
        if (connectorConfig.getEnableAuthorisation()) {
            requestBuilder.addHeader("Authorization", connectorConfig.getAuthType() + " " + connectorConfig.getApiKey());
        }
        return requestBuilder.build();
    }

    public Request generateRequest2(String country) {
        StringBuilder baseUrl = new StringBuilder(connectorConfig.getBaseURL());

        if (!baseUrl.toString().endsWith("/"))
            baseUrl.append("/states/"+country);
        Request.Builder requestBuilder = new Request.Builder()
                .url(baseUrl.toString());
        if (connectorConfig.getEnableAuthorisation()) {
            requestBuilder.addHeader("Authorization", connectorConfig.getAuthType() + " " + connectorConfig.getApiKey());
        }
        return requestBuilder.build();
    }

    public Response generateResponse(Request request) throws IOException {
        return okHttpClient.newCall(request).execute();
    }




}
