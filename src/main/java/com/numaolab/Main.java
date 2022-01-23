package com.numaolab;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;

import com.numaolab.schemas.TagData;
import com.numaolab.transforms.ArchiveToMongoDB;
import com.numaolab.transforms.Cleaning;
import com.numaolab.transforms.EventDetector;
import com.numaolab.transforms.InputFromMqtt;
import com.numaolab.transforms.OutputToMongoDB;

import com.numaolab.schemas.Result;
public class Main {

  public interface MainOptions extends PipelineOptions {
    @Description("ID")
    @Default.String("default")
    String getId();
    void setId(String value);

    @Description("MQTT URL")
    @Default.String("tcp://localhost:1883")
    String getMqttUrl();
    void setMqttUrl(String value);

    @Description("MONGO URL")
    @Default.String("mongodb://root:example@localhost:27017")
    String getMongoUrl();
    void setMongoUrl(String value);

    @Description("ENV")
    @Default.String("")
    String getEnv();
    void setEnv(String value);

    @Description("HEADER")
    @Default.String("")
    String getHeader();
    void setHeader(String value);
  }

  public static void main(String[] args) {
    MainOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(MainOptions.class);

    Pipeline pipeline = Pipeline.create(options);

    /**
     * **********************************************************************************************
     * INPUT
     * **********************************************************************************************
     */
    PCollection<byte[]> data =
      pipeline.apply("INPUT FROM MQTT",
        new InputFromMqtt(options.getMqttUrl(), "all"));

    /**
     * **********************************************************************************************
     * CLEANING
     * **********************************************************************************************
     */
    PCollection<TagData> cleanData =
      data.apply("CLEANING",
        new Cleaning(options.getEnv(), options.getHeader()));

    /**
     * **********************************************************************************************
     * ARCHIVE
     * **********************************************************************************************
     */
    cleanData.apply("ARCHIVE TO MONGODB",
      new ArchiveToMongoDB(options.getMongoUrl(), "lehc" + options.getId(), "log"));

    /**
     * **********************************************************************************************
     * DETECT EVENT
     * **********************************************************************************************
     */
    PCollectionList<Result> resultDataList =
        cleanData.apply("DETECT EVENT", new EventDetector());

    /**
     * **********************************************************************************************
     * OUTPUT
     * **********************************************************************************************
     */
    for (PCollection<Result> p: resultDataList.getAll()) {
      p.apply("OUTPUT",
        new OutputToMongoDB(options.getMongoUrl(), "lehc" + options.getId(), "result"));
    }

    pipeline.run().waitUntilFinish();
  }
}
