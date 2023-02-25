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

package org.pratyush.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.api.data.batch.Input;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.dataset.lib.KeyValue;
import io.cdap.cdap.etl.api.Emitter;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.PipelineConfigurer;
import io.cdap.cdap.etl.api.batch.BatchSource;
import io.cdap.cdap.etl.api.batch.BatchSourceContext;
import io.cdap.plugin.common.SourceInputFormatProvider;
import io.cdap.plugin.common.batch.JobUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Plugin(type = BatchSource.PLUGIN_TYPE)
@Name(LocalFileBatchSource.NAME)
@Description("Reads data from Local File.")
public class LocalFileBatchSource extends BatchSource<LongWritable, Text, StructuredRecord> {

    public static final String NAME = "LocalFile";
    private final LocalFilePluginConfig pluginConfig;

    public static final Schema DEFAULT_SCHEMA;

    static {
        DEFAULT_SCHEMA = Schema.recordOf("event", Schema.Field.of("offset", Schema.of(Schema.Type.LONG)), Schema.Field.of("body", Schema.of(Schema.Type.STRING)));
    }

    public LocalFileBatchSource(LocalFilePluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    @Override
    public void prepareRun(BatchSourceContext batchSourceContext) throws Exception {
        FailureCollector failureCollector = batchSourceContext.getFailureCollector();
        failureCollector.getOrThrowException();
        Schema outputSchema = batchSourceContext.getOutputSchema();

        if (outputSchema == null) {
            outputSchema = getOutputSchema();
        }

        if (outputSchema == null) {
            throw new IllegalArgumentException("Output Schema is Null.");
        }

        Job hadoopJob = JobUtils.createInstance();
        Gson gson = new GsonBuilder().create();
        Configuration jobConfiguration = hadoopJob.getConfiguration();
        jobConfiguration.set(pluginConfig.getReferenceName(), gson.toJson(pluginConfig));

        TextInputFormat.addInputPath(hadoopJob, new Path(new URI(pluginConfig.getFilePath())));
        SourceInputFormatProvider inputFormat = new SourceInputFormatProvider(TextInputFormat.class, jobConfiguration);

        batchSourceContext.setInput(Input.of(pluginConfig.getReferenceName(), inputFormat));
    }

    private Schema getOutputSchema() {
        if (pluginConfig.getGenerateSchemaToggle()) {
            return getSchemaFromHeader();
        } else
            return DEFAULT_SCHEMA;
    }

    private Schema getSchemaFromHeader() {
        List<String> firstColumnValues = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(pluginConfig.getFilePath()))) {
            String line = br.readLine();
            if (line != null) {
                String[] values = line.split(",");
                for (String value : values) {
                    firstColumnValues.add(value.trim());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<Schema.Field> schemaFields = new ArrayList<>();

        for (String headerName : firstColumnValues) {
            schemaFields.add(Schema.Field.of(headerName, Schema.of(Schema.Type.STRING)));
        }


        return Schema.recordOf("event", schemaFields);
    }

    @Override
    public void configurePipeline(PipelineConfigurer pipelineConfigurer) {
        super.configurePipeline(pipelineConfigurer);
        pipelineConfigurer.getStageConfigurer().setOutputSchema(getOutputSchema());
        pluginConfig.validate(pipelineConfigurer.getStageConfigurer().getFailureCollector());

    }

    @Override
    public void transform(KeyValue<LongWritable, Text> input, Emitter<StructuredRecord> emitter) throws Exception {
        StructuredRecord.Builder builder = StructuredRecord.builder(getOutputSchema());

        if (!pluginConfig.includeHeaders()) {
            if (input.getKey().get() == 0) { //skip headers
                return;
            }
        }

        if (pluginConfig.getGenerateSchemaToggle()) {

            int idx = 0;
            String[] valuesSplit = input.getValue().toString().split(pluginConfig.getDelimiter());
            for (Schema.Field f : getOutputSchema().getFields()) {
                builder.set(f.getName(), valuesSplit[idx]);
                idx++;
            }
        } else { //Default
            builder.set(getOutputSchema().getFields().get(0).getName(), input.getKey().get());
            builder.set(getOutputSchema().getFields().get(1).getName(), input.getValue().toString());
        }
        emitter.emit(builder.build());

    }


}