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

import javax.annotation.Nullable;

public class HTTPConnectorConfig extends PluginConfig {
    @Name("baseURL")
    @Description("Enter HTTP Base Url. Ex-https://example.com")
    private final String baseURL;
    @Name("endPoint")
    @Description("Enter endpoint. Ex-users")
    private final String endPoint;

    @Name("enableAuthorisation")
    @Description("Enable if API requires authorisation")
    private Boolean enableAuthorisation;

    @Name("apiKey")
    @Description("Enter api key")
    @Nullable
    private String apiKey;

    @Name("authType")
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

    public Boolean ifRequiresAuth() {
        return enableAuthorisation;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getAuthType() {
        return authType;
    }
}
