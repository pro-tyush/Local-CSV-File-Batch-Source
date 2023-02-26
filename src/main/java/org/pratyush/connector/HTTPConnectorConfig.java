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

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.FailureCollector;

import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import java.net.URISyntaxException;

public class HTTPConnectorConfig extends PluginConfig {
    public static final String NAME_BASE_URL = "baseURL";
    public static final String NAME_ENDPOINT = "endPoint";
    public static final String NAME_ENABLE_AUTH = "enableAuthorisation";
    public static final String NAME_API_KEY = "apiKey";
    public static final String NAME_AUTH_TYPE = "authType";

    @Name(NAME_BASE_URL)
    @Description("Enter HTTP Base Url. Ex-https://example.com")
    private final String baseURL;
    @Name(NAME_ENDPOINT)
    @Description("Enter endpoint. Ex-users/")
    private final String endPoint;

    @Name(NAME_ENABLE_AUTH)
    @Description("Enable if API requires authorisation")
    private Boolean enableAuthorisation;

    @Name(NAME_API_KEY)
    @Description("Enter api key")
    @Nullable
    private String apiKey;

    @Name(NAME_AUTH_TYPE)
    @Description("Select authentication type")
    @Nullable
    private String authType;

    public HTTPConnectorConfig(String baseURL, String endPoint, Boolean enableAuthorisation, String apiKey, String authType) {
        this.baseURL = baseURL;
        this.endPoint = endPoint;
        this.enableAuthorisation = enableAuthorisation;
        this.apiKey = apiKey;
        this.authType = authType;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public Boolean isAuthReqd() {
        return enableAuthorisation;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getAuthType() {
        return authType;
    }

    public void validateConnectorParams(FailureCollector failureCollector) {
        //TODO Check why these don't work
        if (baseURL.isEmpty() || baseURL == null)
            failureCollector.addFailure("Base url is empty.", "Enter valid http url.");
        if (endPoint.isEmpty() || endPoint == null)
            failureCollector.addFailure("Endpoint is empty.", "Enter valid http endpoint or set default endpoint as '/'");
        if (isAuthReqd() && (apiKey.isEmpty() || apiKey == null))
            failureCollector.addFailure("Enter auth parameters", null);
        HttpGsonHandler httpGsonHandler = new HttpGsonHandler(this);
        try {
            if (httpGsonHandler.isValidHttpString(baseURL)){
                    //no-op
            }
        } catch (URISyntaxException e) {
            failureCollector.addFailure(e.getMessage(),"Base Url isn't HTTP type.");
        }

    }
}
