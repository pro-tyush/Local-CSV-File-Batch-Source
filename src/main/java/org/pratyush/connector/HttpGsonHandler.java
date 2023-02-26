/*
 * Copyright © 2023 Cask Data, Inc.
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpGsonHandler {
    private static final OkHttpClient okHttpClient;
    private static final Gson gson;
    private static final String AUTH_HEADER = "Authorization";

    static {
        okHttpClient = new OkHttpClient();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();
    }

    private HTTPConnectorConfig connectorConfig;

    public HttpGsonHandler(HTTPConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
    }

    public Gson getGsonObj() {
        return gson;
    }

    public Request generateRequest(String url) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);
        if (connectorConfig.isAuthReqd()) {
            requestBuilder.addHeader(AUTH_HEADER, connectorConfig.getAuthType() + " " + connectorConfig.getApiKey());
        }
        return requestBuilder.build();
    }

    public Response generateResponse(Request request) throws IOException {
        return okHttpClient.newCall(request).execute();
    }

    public String cleanUrl(String url) {
        String regex = "(?<!(http:|https:))" + HTTPConnector.PATH_SEPARATOR + "{2,}";
        return url.replaceAll(regex, HTTPConnector.PATH_SEPARATOR);
        //cleans url by removing extra separators except when preceded by http or https. http://a.com///b -> http://a.com/b
    }

    public boolean isValidHttpString(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return false;
        }
        if (uri.getHost() == null) {
            return false;
        }
        if (uri.getPath() == null) {
            return false;
        }
        return true;
    }
}
