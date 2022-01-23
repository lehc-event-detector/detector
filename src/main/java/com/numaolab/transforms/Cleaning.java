package com.numaolab.transforms;

import com.numaolab.Config;
import com.numaolab.schemas.TagData;

import org.apache.beam.sdk.schemas.NoSuchSchemaException;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.transforms.Convert;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.JsonToRow;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.joda.time.Duration;
import org.joda.time.Instant;

public class Cleaning extends PTransform<PCollection<byte[]>, PCollection<TagData>>  {
  private final String env;
  private final String header;
  private static final Duration timeSkew = Config.timeSkew;

  public Cleaning(String env, String header) {
    this.env = env;
    this.header = header;
  }

  protected static class AddTimestamp extends DoFn<TagData, TagData> {
    @ProcessElement
    public void process(@Element TagData e, OutputReceiver<TagData> o, @Timestamp Instant t) {
      Instant nt = Instant.ofEpochMilli(Long.parseLong(e.getTime())/1000).plus(timeSkew);
      if (t.isBefore(nt)) {
        o.outputWithTimestamp(e, nt);
      }
    }
  }

  @Override
  public PCollection<TagData> expand(PCollection<byte[]> messages) {
    /**
     * **********************************************************************************************
     * Create TagData Schema
     * **********************************************************************************************
     */
    Schema tagDataSchema = null;
    try {
      tagDataSchema = messages.getPipeline().getSchemaRegistry().getSchema(TagData.class);
    } catch (NoSuchSchemaException e) {
      throw new IllegalArgumentException("Unable to get Schema for TagData class.");
    }

    /**
     * **********************************************************************************************
     * Byte[] to String
     * **********************************************************************************************
     */
    PCollection<String> jsonStrings =
      messages.apply("byte to String", ParDo.of(
        new DoFn<byte[], String>() {
          @ProcessElement
          public void process(ProcessContext c) {
            c.output(new String(c.element()));
          }
        }
      ));

    /**
     * **********************************************************************************************
     * String to Row
     * **********************************************************************************************
     */
    PCollection<Row> tagDataRows =
      jsonStrings.apply("String to Row", JsonToRow.withSchema(tagDataSchema));

    /**
     * **********************************************************************************************
     * Row to TagData Schema
     * **********************************************************************************************
     */
    PCollection<TagData> tagDataSchemas =
      tagDataRows.apply("Row to TagData Schema", Convert.to(TagData.class));

    /**
     * **********************************************************************************************
     * Filter by Env
     * **********************************************************************************************
     */
    PCollection<TagData> envedTagDataSchemas =
      tagDataSchemas.apply("Filter by Env", ParDo.of(
        new DoFn<TagData, TagData>() {
          @ProcessElement
          public void process(ProcessContext c) {
            if (env.equals("")) {
              c.output(c.element());
            } else if (c.element().getEnv().equals(env)) {
              c.output(c.element());
            }
          }
        }
      ));

    /**
     * **********************************************************************************************
     * Filter by Header
     * **********************************************************************************************
     */
    PCollection<TagData> headeredTagDataSchemas =
      envedTagDataSchemas.apply("Filter by Header", ParDo.of(
        new DoFn<TagData, TagData>() {
          @ProcessElement
          public void process(ProcessContext c) {
            if (header.equals("")) {
              c.output(c.element());
            } else if (c.element().getHeader().equals(header)) {
              c.output(c.element());
            }
          }
        }
      ));

    /**
     * **********************************************************************************************
     * Add timestamp
     * **********************************************************************************************
     */
    PCollection<TagData> stampedTagData =
      headeredTagDataSchemas.apply(
          ParDo.of(new AddTimestamp()));

    return stampedTagData;
  }
}
