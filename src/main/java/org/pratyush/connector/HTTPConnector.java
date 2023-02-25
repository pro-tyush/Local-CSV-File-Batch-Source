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
import com.google.gson.reflect.TypeToken;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.connector.*;
import io.cdap.cdap.etl.api.validation.ValidationException;
import okhttp3.Request;
import okhttp3.Response;
import org.pratyush.plugin.LocalFileBatchSource;
import org.pratyush.plugin.LocalFilePluginConfig;
import org.pratyush.connector.entities.LocalFileEntity;
import org.pratyush.util.CsvHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Connector.PLUGIN_TYPE)
@Name(HTTPConnector.NAME)
@Description("Access data from HTTP, compatible with nodeJS Server which exposes local file system.")
public class HTTPConnector implements DirectConnector {
    public static final String NAME = "HTTP";
    private final HttpGsonHandler okHttpHandler;
    private String baseUrl;

    private final HTTPConnectorConfig connectorConfig;
    private static final String ENDPOINT_PATH_SEPARATOR = "/";
    private static final String CSV_EXT = ".csv";

    private static final int MAX_SAMPLE_LIMIT = 1000;


    public HTTPConnector(HTTPConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
        okHttpHandler = new HttpGsonHandler(connectorConfig);
        baseUrl = getBaseUrl(connectorConfig.getBaseURL());
    }

    private String getBaseUrl(String baseUrl) {
        if (!baseUrl.endsWith(ENDPOINT_PATH_SEPARATOR)) {
            return baseUrl + ENDPOINT_PATH_SEPARATOR;
        }
        return baseUrl;
    }


    @Override
    public void test(ConnectorContext connectorContext) throws ValidationException {
        FailureCollector collector = connectorContext.getFailureCollector();
        Request request = okHttpHandler.generateRequest(baseUrl + connectorConfig.getEndPoint());
        try (Response response = okHttpHandler.generateResponse(request)) {
            if (!response.isSuccessful()) {
                collector.addFailure("Request Failed", "Check BaseUrl, Endpoint" + (connectorConfig.ifRequiresAuth() ? " and Auth" : ""));
            }
        } catch (IOException e) {
            collector.addFailure(e.getMessage(), null);
        }
    }


    public BrowseDetail browse(ConnectorContext connectorContext, BrowseRequest browseRequest) throws IOException {
        String path = browseRequest.getPath();
        Request request = okHttpHandler.generateRequest(baseUrl + ENDPOINT_PATH_SEPARATOR + path);
        Response response = okHttpHandler.generateResponse(request);
        String responseString = response.body().string();
        Gson gson = okHttpHandler.getGsonObj();

        List<LocalFileEntity> fileEntities = gson.fromJson(responseString, new TypeToken<List<LocalFileEntity>>() {
        }.getType());
        BrowseDetail.Builder builder = BrowseDetail.builder();
        for (LocalFileEntity fileEntity : fileEntities) {
            String separator = path.endsWith(ENDPOINT_PATH_SEPARATOR) ? "" : ENDPOINT_PATH_SEPARATOR;
            String entityPath = path + separator + fileEntity.getName();
            String entityType = fileEntity.isDir() ? "directory" : "file";
            BrowseEntity.Builder entity = BrowseEntity.builder(fileEntity.getName(), entityPath, entityType)
                    .canBrowse(fileEntity.isDir())
                    .canSample(!fileEntity.isDir());
            builder.addEntity(entity.build());
        }

        return builder.setTotalCount(fileEntities.size()).build();
    }

    @Override
    public ConnectorSpec generateSpec(ConnectorContext connectorContext, ConnectorSpecRequest connectorSpecRequest) {
        String localPath = connectorSpecRequest.getPath();
        Map<String, String> pluginProps = new HashMap<>();
        pluginProps.put(LocalFilePluginConfig.NAME_REFERENCE_NAME, localPath.substring(localPath.lastIndexOf(ENDPOINT_PATH_SEPARATOR) + 1));
        pluginProps.put(LocalFilePluginConfig.NAME_FILE_PATH, localPath);

        PluginSpec pluginSpec = new PluginSpec(LocalFileBatchSource.NAME, LocalFileBatchSource.PLUGIN_TYPE, pluginProps);
        return ConnectorSpec.builder().addRelatedPlugin(pluginSpec).build();
    }

    @Override
    public List<StructuredRecord> sample(ConnectorContext connectorContext, SampleRequest sampleRequest) throws IOException {
        Request request = okHttpHandler.generateRequest(baseUrl + sampleRequest.getPath());
        Response response = okHttpHandler.generateResponse(request);
        String responseString = response.body().string();
        boolean isCsv = sampleRequest.getPath().endsWith(CSV_EXT);
        if (isCsv)
            return sampleCsv(responseString);
        else
            return sampleFile(responseString);
    }

    private List<StructuredRecord> sampleCsv(String csvString) {
        String[] lines = csvString.split("\n");
        CsvHelper csvHelper = new CsvHelper();
        Schema schema = csvHelper.generateSchemaFromCsv(csvString, ",");
        List<StructuredRecord> structuredRecordList = new ArrayList<>();
        int limit = Math.min(lines.length, MAX_SAMPLE_LIMIT);
        for (int i = 1; i < limit; i++) {
            String[] splitComma = lines[i].split(",");
            StructuredRecord.Builder builder = StructuredRecord.builder(schema);
            for (int j = 0; j < splitComma.length; j++) {
                builder.set(schema.getFields().get(j).getName(), splitComma[j]);
            }
            structuredRecordList.add(builder.build());
        }
        return structuredRecordList;
    }

    private List<StructuredRecord> sampleFile(String responseString) {
        String[] lines = responseString.split("\n");
        long offset = 0;
        Schema schema = LocalFileBatchSource.DEFAULT_SCHEMA;
        List<StructuredRecord> structuredRecordList = new ArrayList<>();
        int limit = Math.min(lines.length, MAX_SAMPLE_LIMIT);

        for (int i = 0; i < limit; i++) {
            StructuredRecord.Builder builder = StructuredRecord.builder(schema);
            builder.set(schema.getFields().get(0).getName(), offset);
            builder.set(schema.getFields().get(1).getName(), lines[i]);
            offset += lines[i].length();
            structuredRecordList.add(builder.build());
        }
        return structuredRecordList;
    }


}