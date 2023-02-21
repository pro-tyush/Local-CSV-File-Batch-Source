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

package org.pratyush.config;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.FailureCollector;

import java.io.File;

public class LocalFilePluginConfig extends PluginConfig {
    @Name("referenceName")
    @Description("This will be used to uniquely identify this source/sink for lineage, annotating metadata, etc.")
    public String referenceName;

    @Name("filePath")
    @Description("Enter Local File Path.")
    private String filePath;

    @Name("generateSchemaToggle")
    @Description("Generate Schema From CSV Header(i.e. First Column)")
    private Boolean generateSchemaToggle;

    @Name("headersToggle")
    @Description("This will include headers data in CSV. Considers 1st column as header")
    private Boolean headersToggle;

    @Name("delimiter")
    @Description("Choose delimiter symbol used in CSV File.")
    private String delimiter;


    public LocalFilePluginConfig(String referenceName, String filePath, Boolean generateSchemaToggle, Boolean headersToggle, String delimiter) {
        this.referenceName = referenceName;
        this.filePath = filePath;
        this.generateSchemaToggle = generateSchemaToggle;
        this.headersToggle = headersToggle;
        this.delimiter = delimiter;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public String getFilePath() {
        return filePath;
    }

    public Boolean getGenerateSchemaToggle() {
        return generateSchemaToggle;
    }

    public Boolean includeHeaders() {
        return headersToggle;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void validate(FailureCollector failureCollector) {
        failureCollector.getOrThrowException();
    }

}
