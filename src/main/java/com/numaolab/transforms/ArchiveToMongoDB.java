package com.numaolab.transforms;

import com.numaolab.schemas.TagData;

import org.apache.beam.sdk.io.mongodb.MongoDbIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.bson.Document;

public class ArchiveToMongoDB extends PTransform<PCollection<TagData>, PDone> {
  private static final String template = "{\"header\":\"%s\",\"other\":\"%s\",\"env\":\"%s\",\"eri\":\"%s\",\"logic\":\"%s\",\"k\":\"%s\",\"kd\":\"%s\",\"gid\":\"%s\",\"mbit\":\"%s\",\"igs\":\"%s\",\"rssi\":\"%s\",\"phase\":\"%s\",\"time\":\"%s\"}";
  private final String uri;
  private final String dbName;
  private final String colName;

  public ArchiveToMongoDB(String uri, String dbName, String colName) {
    this.uri = uri;
    this.dbName = dbName;
    this.colName = colName;
  }

  @Override
  public PDone expand(PCollection<TagData> input) {
    /**
     * **********************************************************************************************
     * TagData to BSON
     * **********************************************************************************************
     */
    PCollection<Document> docs =
      input.apply("TagData to BSON", ParDo.of(
        new DoFn<TagData, Document>() {
          @ProcessElement
          public void process(ProcessContext c) {
            TagData e = c.element();
            String json = String.format(
              template, e.getHeader(), e.getOther(), e.getEnv(), e.getEri(), e.getLogic(), e.getK(), e.getKd(), e.getGid(), e.getMbit(), e.getIgs(), e.getRssi(), e.getPhase(), e.getTime());
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
