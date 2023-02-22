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

import io.cdap.cdap.api.annotation.Category;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.etl.api.connector.*;
import io.cdap.cdap.etl.api.validation.ValidationException;
import okhttp3.*;
import org.pratyush.LocalFileBatchSource;

import java.io.IOException;

@Plugin(type = Connector.PLUGIN_TYPE)
@Name(LocalFileBatchSource.NAME)
@Category("REST Connect")
@Description("Access data from countries REST API")
public class LocalFileConnector implements Connector {
    public static final String NAME = "RESTConnector";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjp7InVzZXJfZW1haWwiOiJwcmF0eXVzaHNob3BwaW5nQGdtYWlsLmNvbSIsImFwaV90b2tlbiI6Ik5GYkh0YlNMdHpfbWtqci02S3l3WVUtcE5iZ2Zzakl2WGx0bWxPVi1ORUIwRVRRNW91ZmhQRW1QeFNOZE8zMC1FQ2MifSwiZXhwIjoxNjc3MTQzMTE2fQ.bdIYzHZr4efcn_3wW2sbZq4a5eVFxL0x5LJ8Jo3Y0HY";

    private final LocalFileConnectorConfig config;


    public LocalFileConnector(LocalFileConnectorConfig config) {
        this.config = config;
    }


    @Override
    public void test(ConnectorContext connectorContext) throws ValidationException {

    }

    @Override
    public BrowseDetail browse(ConnectorContext connectorContext, BrowseRequest browseRequest) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();

        Request countriesRequest = new Request.Builder()
                .url("https://www.universal-tutorial.com/api/countries")
                .header("Authorization", "Bearer " + API_KEY)
                .build();

        okHttpClient.newCall(countriesRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    //
                }
            }
        });
        BrowseDetail browseDetail = BrowseDetail.builder().build();
        return browseDetail;
    }

    @Override
    public ConnectorSpec generateSpec(ConnectorContext connectorContext, ConnectorSpecRequest connectorSpecRequest) throws IOException {
        return null;
    }


}
