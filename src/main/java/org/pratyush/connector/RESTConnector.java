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
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.connector.*;
import io.cdap.cdap.etl.api.validation.ValidationException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.pratyush.connector.entities.LocalFileEntity;

import java.io.IOException;
import java.util.List;

@Plugin(type = Connector.PLUGIN_TYPE)
@Name(RESTConnector.NAME)
@Description("Access data from countries REST API")
public class RESTConnector implements DirectConnector {
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
                //validation
                collector.addFailure("Request Failed", "Check BaseUrl, Endpoint and Auth(if enabled).");
            }
        } catch (IOException e) {
            collector.addFailure(e.getMessage(), null);
        }

    }

    public BrowseDetail browse(ConnectorContext connectorContext, BrowseRequest browseRequest) throws IOException {
        String path = browseRequest.getPath();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://localhost:3000/" + path)
                .build();
        Response response = client.newCall(request).execute();
        String jsonResponse = response.body().string();

        Gson gson = new GsonBuilder().create();
        LocalFileEntity[] fileEntities = gson.fromJson(jsonResponse, LocalFileEntity[].class);
        BrowseDetail.Builder builder = BrowseDetail.builder();
        for (LocalFileEntity fileEntity : fileEntities) {
            BrowseEntity.Builder entity =
                    BrowseEntity.builder(
                                    fileEntity.getName(), path + "/" + fileEntity.getName(), fileEntity.isDir() ? "directory" : "file")
                            .canBrowse(fileEntity.isDir())
                            .canSample(!fileEntity.isDir());
            builder.addEntity(entity.build());
        }
        return builder.setTotalCount(fileEntities.length).build();

    }

//    @Override
//    public BrowseDetail browse(ConnectorContext connectorContext, BrowseRequest browseRequest) throws IOException {
//        BrowseDetail.Builder browseDetailBuilder = BrowseDetail.builder();
//
//        OkHttpHandler okHttpHandler = new OkHttpHandler(connectorConfig);
//        Request request = okHttpHandler.generateRequest();
//        Response response = okHttpHandler.generateResponse(request);
//        String responseString = response.body().string();
//
//        String path = browseRequest.getPath();
//        if(path.length() < 2) return browseCountry(responseString, browseDetailBuilder);
//
//        request = okHttpHandler.generateRequest2(path.split("/")[1]);
//        response = okHttpHandler.generateResponse(request);
//        responseString = response.body().string();
//        return browseState(responseString, browseDetailBuilder);
//
//
//    }
//
//    public BrowseDetail browseCountry(String responseString, BrowseDetail.Builder browseDetailBuilder) {
//        Gson gson = OkHttpHandler.getGsonObj();
//        CountryEntity[] countryEntities = gson.fromJson(responseString, CountryEntity[].class);
//        for (CountryEntity countryEntity : countryEntities) {
//            BrowseEntity.Builder entityBuilder = BrowseEntity.builder(
//                    countryEntity.getCountry_name(), "/" + countryEntity.getCountry_name(), "directory"
//            ).canSample(false).canBrowse(true);
//            browseDetailBuilder.addEntity(entityBuilder.build());
//        }
//        return browseDetailBuilder.setTotalCount(countryEntities.length).build();
//    }
//
//    public BrowseDetail browseState(String responseString, BrowseDetail.Builder browseDetailBuilder) {
//        Gson gson = OkHttpHandler.getGsonObj();
//        StateEntity[] stateEntities = gson.fromJson(responseString, StateEntity[].class);
//        for (StateEntity stateEntity : stateEntities) {
//            BrowseEntity.Builder entityBuilder = BrowseEntity.builder(
//                    stateEntity.getState_name(), stateEntity.getState_name(), "file"
//            ).canSample(true).canBrowse(false);
//            browseDetailBuilder.addEntity(entityBuilder.build());
//        }
//        return browseDetailBuilder.setTotalCount(stateEntities.length).build();
//    }
//
//    public BrowseDetail browseFile() {
//        return null;
//    }


    @Override
    public ConnectorSpec generateSpec(ConnectorContext connectorContext, ConnectorSpecRequest connectorSpecRequest) throws IOException {
        return null;
    }

    @Override
    public List<StructuredRecord> sample(ConnectorContext connectorContext, SampleRequest sampleRequest) throws IOException {
        return null;
    }


}
