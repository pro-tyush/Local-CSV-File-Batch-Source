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

package org.pratyush.util;

import io.cdap.cdap.api.data.schema.Schema;

import java.util.ArrayList;
import java.util.List;

public class CsvHelper {

    public static final String CSV_EXT = ".csv";
    public Schema generateSchemaFromCsv(String csvString, String delimiter) {
        String headerLine = csvString.split("\n")[0]; //if csvString has multiple lines consider first
        String[] headerValues = headerLine.split(delimiter);

        List<Schema.Field> schemaFields = new ArrayList<>();

        for (String headerName : headerValues) {
            schemaFields.add(Schema.Field.of(headerName, Schema.of(Schema.Type.STRING)));
        }
        return Schema.recordOf("event", schemaFields);
    }

    public boolean isCsvFile(String path){
        return path.endsWith(CSV_EXT);
    }
}

