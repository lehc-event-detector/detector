package com.numaolab.transforms;

import com.numaolab.schemas.Result;

import org.apache.beam.sdk.io.mongodb.MongoDbIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.bson.Document;

public class OutputToMongoDB extends PTransform<PCollection<Result>, PDone> {
  private static final String template = "{\"gid\": \"%s\", \"logic\": \"%s\", \"detectedAt\": \"%s\"}";
  private final String uri;
  private final String dbName;
  private final String colName;

  public OutputToMongoDB(String uri, String dbName, String colName) {
    this.uri = uri;
    this.dbName = dbName;
    this.colName = colName;
  }

  @Override
  public PDone expand(PCollection<Result> input) {
    /**
     * **********************************************************************************************
     * Result to BSON
     * **********************************************************************************************
     */
    PCollection<Document> docs =
      input.apply("Result to BSON", ParDo.of(
        new DoFn<Result, Document>() {
          @ProcessElement
          public void process(ProcessContext c) {
            Result e = c.element();
            String json = String.format(
              template, e.getGid(), e.getLogic().toString(), e.getDetectedAt().getMillis() + "999");
            c.output(Document.parse(json));
          }
        }
      ));

    /**
     * **********************************************************************************************
     * Write to MongoDB
     * **********************************************************************************************
     */
    docs.apply(
      MongoDbIO
        .write()
        .withUri(uri)
        .withDatabase(dbName)
        .withCollection(colName));

    return PDone.in(input.getPipeline());
  }
}
