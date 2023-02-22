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

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.connector.*;
import io.cdap.cdap.etl.api.validation.ValidationException;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@Plugin(type = Connector.PLUGIN_TYPE)
@Name(RESTConnector.NAME)
@Description("Access data from countries REST API")
public class RESTConnector implements Connector {
    public static final String NAME = "REST";

    private final RESTConnectorConfig connectorConfig;

    public RESTConnector(RESTConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
    }

    @Override
    public void test(ConnectorContext connectorContext) throws ValidationException{
        FailureCollector collector = connectorContext.getFailureCollector();
        OkHttpHandler okHttpHandler = new OkHttpHandler(connectorConfig);
        Request request = okHttpHandler.generateRequest();
        try {
            Response response = okHttpHandler.generateResponse(request);
            if (!response.isSuccessful()){
                collector.addFailure("Request Failed","Check BaseUrl, Endpoint and Auth(if enabled).");
            }
        } catch (IOException e) {
            collector.addFailure(e.getMessage(),null);
        }

    }

    @Override
    public BrowseDetail browse(ConnectorContext connectorContext, BrowseRequest browseRequest) throws IOException {
        BrowseDetail.Builder browseDetailBuilder = BrowseDetail.builder();
        OkHttpHandler okHttpHandler = new OkHttpHandler(connectorConfig);
        Request request = okHttpHandler.generateRequest();
        Response response = okHttpHandler.generateResponse(request);
        String name = response.body().toString();
        BrowseEntity.Builder entity = (BrowseEntity.builder(name, name, "template").
                canBrowse(true).canSample(false));
        browseDetailBuilder.addEntity(entity.build());
        return browseDetailBuilder.build();
    }

    @Override
    public ConnectorSpec generateSpec(ConnectorContext connectorContext, ConnectorSpecRequest connectorSpecRequest) throws IOException {
        return null;
    }

}
