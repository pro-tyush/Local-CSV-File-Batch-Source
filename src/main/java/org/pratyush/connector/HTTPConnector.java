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

    private String endPoint;
    private final HTTPConnectorConfig connectorConfig;
    public static final String PATH_SEPARATOR = "/";
    private static final int MAX_SAMPLE_LIMIT = 1000;

    private CsvHelper csvHelper;

    public HTTPConnector(HTTPConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
        okHttpHandler = new HttpGsonHandler(connectorConfig);
        baseUrl = getBaseUrl(connectorConfig.getBaseURL());
        endPoint = connectorConfig.getEndPoint();
        csvHelper = new CsvHelper();
    }

    private String getBaseUrl(String baseUrl) {
        if (!baseUrl.endsWith(PATH_SEPARATOR)) {
            return baseUrl + PATH_SEPARATOR;
        }
        return baseUrl;
    }

    @Override
    public void test(ConnectorContext connectorContext) throws ValidationException {
        FailureCollector collector = connectorContext.getFailureCollector();
        connectorConfig.validateConnectorParams(collector);
        if (!collector.getValidationFailures().isEmpty())
            return; //if errors found stop execution
        Request request = okHttpHandler.generateRequest(okHttpHandler.cleanUrl(baseUrl + connectorConfig.getEndPoint()));
        try {
            Response response = okHttpHandler.generateResponse(request);
            if (!response.isSuccessful())
                collector.addFailure("Request Failed", "Check BaseUrl, Endpoint" + (connectorConfig.isAuthReqd() ? " and Auth" : ""));

        } catch (IOException e) {
            collector.addFailure(e.getMessage(), null);
        }
    }


    public BrowseDetail browse(ConnectorContext connectorContext, BrowseRequest browseRequest) throws IOException {
        String path = browseRequest.getPath();
        path = path.equals(PATH_SEPARATOR) ? path + endPoint : path; //redirect "/" to endPoint, note: default endPoint is /
        Request request = okHttpHandler.generateRequest(okHttpHandler.cleanUrl(baseUrl + PATH_SEPARATOR + path));
        Response response = okHttpHandler.generateResponse(request);
        String responseString = response.body().string();
        Gson gson = okHttpHandler.getGsonObj();

        List<LocalFileEntity> fileEntities = gson.fromJson(responseString, new TypeToken<List<LocalFileEntity>>() {
        }.getType());
        BrowseDetail.Builder builder = BrowseDetail.builder();
        for (LocalFileEntity fileEntity : fileEntities) {
            String separator = path.endsWith(PATH_SEPARATOR) ? "" : PATH_SEPARATOR;
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
        pluginProps.put(LocalFilePluginConfig.NAME_REFERENCE_NAME, localPath.substring(localPath.lastIndexOf(PATH_SEPARATOR) + 1));
        pluginProps.put(LocalFilePluginConfig.NAME_FILE_PATH, okHttpHandler.cleanUrl(localPath));
        //TODO Add options windows (delimiter etc)
        PluginSpec pluginSpec = new PluginSpec(LocalFileBatchSource.NAME, LocalFileBatchSource.PLUGIN_TYPE, pluginProps);
        Schema pluginSchema = generateSchemaForPlugin(localPath, connectorContext.getFailureCollector());

        return ConnectorSpec.builder().setSchema(pluginSchema).addRelatedPlugin(pluginSpec).build();
    }

    private Schema generateSchemaForPlugin(String path, FailureCollector collector) {
        Request request = okHttpHandler.generateRequest(baseUrl + path);
        try {
            Response response = okHttpHandler.generateResponse(request);
            if (csvHelper.isCsvFile(path))
                return csvHelper.generateSchemaFromCsv(response.body().string(), ",");
        } catch (IOException e) {
            collector.addFailure(e.getMessage(), null);
        }
        return LocalFileBatchSource.DEFAULT_SCHEMA;
    }

    @Override
    public List<StructuredRecord> sample(ConnectorContext connectorContext, SampleRequest sampleRequest) throws
            IOException {
        Request request = okHttpHandler.generateRequest(baseUrl + sampleRequest.getPath());
        Response response = okHttpHandler.generateResponse(request);
        String responseString = response.body().string();
        String[] responseLines = responseString.split("\n");

        int sampleLimit = Math.min(responseLines.length, MAX_SAMPLE_LIMIT);
        if (csvHelper.isCsvFile(sampleRequest.getPath()))
            return sampleCsv(responseLines, sampleLimit);
        else
            return sampleFile(responseLines, sampleLimit);
    }

    private List<StructuredRecord> sampleCsv(String[] csvLines, int sampleLimit) {
        Schema schema = csvHelper.generateSchemaFromCsv(csvLines[0], ",");
        List<StructuredRecord> structuredRecordList = new ArrayList<>();
        for (int i = 1; i < sampleLimit; i++) {
            String[] splitValues = csvLines[i].split(",");
            StructuredRecord.Builder builder = StructuredRecord.builder(schema);
            for (int j = 0; j < splitValues.length; j++) {
                builder.set(schema.getFields().get(j).getName(), splitValues[j]);
            }
            structuredRecordList.add(builder.build());
        }
        return structuredRecordList;
    }

    private List<StructuredRecord> sampleFile(String[] responseLines, int sampleLimit) {
        long offset = 0;
        Schema schema = LocalFileBatchSource.DEFAULT_SCHEMA;
        List<StructuredRecord> structuredRecordList = new ArrayList<>();

        for (int i = 0; i < sampleLimit; i++) {
            StructuredRecord.Builder builder = StructuredRecord.builder(schema);
            builder.set(schema.getFields().get(0).getName(), offset);
            builder.set(schema.getFields().get(1).getName(), responseLines[i]);
            offset += responseLines[i].length();
            structuredRecordList.add(builder.build());
        }
        return structuredRecordList;
    }

}
