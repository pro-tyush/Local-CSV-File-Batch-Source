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

package org.pratyush.plugin;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.FailureCollector;
import org.pratyush.util.CsvHelper;

import java.io.File;

public class LocalFilePluginConfig extends PluginConfig {
    public static final String NAME_REFERENCE_NAME = "referenceName";
    public static final String NAME_FILE_PATH = "filePath";
    public static final String NAME_GEN_SCHEMA_TOGGLE = "generateSchemaToggle";
    public static final String NAME_HEADERS_TOGGLE = "headersToggle";
    public static final String NAME_DELIMITER = "delimiter";

    @Name(NAME_REFERENCE_NAME)
    @Description("This will be used to uniquely identify this source/sink for lineage, annotating metadata, etc.")
    public String referenceName;

    @Name(NAME_FILE_PATH)
    @Description("Enter Local File Path.")
    private String filePath;

    @Name(NAME_GEN_SCHEMA_TOGGLE)
    @Description("Generate Schema From CSV Header(i.e. First Column)")
    private Boolean generateSchemaToggle;

    @Name(NAME_HEADERS_TOGGLE)
    @Description("This will include headers data in CSV. Considers 1st column as header")
    private Boolean headersToggle;

    @Name(NAME_DELIMITER)
    @Description("Choose delimiter symbol used in CSV File.")
    private String delimiter;

    private Schema schema;

    public LocalFilePluginConfig(String referenceName, String filePath, Boolean generateSchemaToggle, Boolean headersToggle, String delimiter, Schema schema) {
        this.referenceName = referenceName;
        this.filePath = filePath;
        this.generateSchemaToggle = generateSchemaToggle;
        this.headersToggle = headersToggle;
        this.delimiter = delimiter;
        this.schema = schema;
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
        //TODO Dynamically show delimiter field, only if file type CSV
        CsvHelper csvHelper = new CsvHelper();
        if (generateSchemaToggle && !csvHelper.isCsvFile(filePath)) {
            failureCollector.addFailure("Can not generate schema.", "File is not of type CSV.");
        }
        if (includeHeaders() && !csvHelper.isCsvFile(filePath)) {
            failureCollector.addFailure("Cannot include Headers", "File is not of type CSV.");
        }
    }

}
