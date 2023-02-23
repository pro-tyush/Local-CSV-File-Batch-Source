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
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.connector.*;
import io.cdap.cdap.etl.api.validation.ValidationException;
import okhttp3.Request;
import okhttp3.Response;
import org.pratyush.connector.entities.CountryEntity;
import org.pratyush.connector.entities.StateEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

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
    public void test(ConnectorContext connectorContext) throws ValidationException {
        FailureCollector collector = connectorContext.getFailureCollector();
        OkHttpHandler okHttpHandler = new OkHttpHandler(connectorConfig);
        Request request = okHttpHandler.generateRequest();
        try {
            Response response = okHttpHandler.generateResponse(request);
            if (!response.isSuccessful()) {
                collector.addFailure("Request Failed", "Check BaseUrl, Endpoint and Auth(if enabled).");
            }
        } catch (IOException e) {
            collector.addFailure(e.getMessage(), null);
        }

    }

    @Override
    public BrowseDetail browse(ConnectorContext connectorContext, BrowseRequest browseRequest) throws IOException {
        BrowseDetail.Builder browseDetailBuilder = BrowseDetail.builder();

        OkHttpHandler okHttpHandler = new OkHttpHandler(connectorConfig);
        Request request = okHttpHandler.generateRequest();
        Response response = okHttpHandler.generateResponse(request);
        String responseString = response.body().string();

        String path = browseRequest.getPath();
        if(path.length() < 2) return browseCountry(responseString, browseDetailBuilder);

        request = okHttpHandler.generateRequest2(path.split("/")[1]);
        response = okHttpHandler.generateResponse(request);
        responseString = response.body().string();
        return browseState(responseString, browseDetailBuilder);


    }

    public BrowseDetail browseCountry(String responseString, BrowseDetail.Builder browseDetailBuilder) {
        Gson gson = OkHttpHandler.getGsonObj();
        CountryEntity[] countryEntities = gson.fromJson(responseString, CountryEntity[].class);
        for (CountryEntity countryEntity : countryEntities) {
            BrowseEntity.Builder entityBuilder = BrowseEntity.builder(
                    countryEntity.getCountry_name(), "/" + countryEntity.getCountry_name(), "directory"
            ).canSample(false).canBrowse(true);
            browseDetailBuilder.addEntity(entityBuilder.build());
        }
        return browseDetailBuilder.setTotalCount(countryEntities.length).build();
    }

    public BrowseDetail browseState(String responseString, BrowseDetail.Builder browseDetailBuilder) {
        Gson gson = OkHttpHandler.getGsonObj();
        StateEntity[] stateEntities = gson.fromJson(responseString, StateEntity[].class);
        for (StateEntity stateEntity : stateEntities) {
            BrowseEntity.Builder entityBuilder = BrowseEntity.builder(
                    stateEntity.getState_name(), stateEntity.getState_name(), "file"
            ).canSample(true).canBrowse(false);
            browseDetailBuilder.addEntity(entityBuilder.build());
        }
        return browseDetailBuilder.setTotalCount(stateEntities.length).build();
    }

    public BrowseDetail browseFile() {
        return null;
    }


    @Override
    public ConnectorSpec generateSpec(ConnectorContext connectorContext, ConnectorSpecRequest connectorSpecRequest) throws IOException {
        return null;
    }

}
