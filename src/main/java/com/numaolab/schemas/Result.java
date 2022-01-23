package com.numaolab.schemas;

import com.google.auto.value.AutoValue;
import com.numaolab.Config;
import com.numaolab.enums.Logic;

import org.apache.beam.sdk.schemas.AutoValueSchema;
import org.apache.beam.sdk.schemas.annotations.DefaultSchema;
import org.apache.beam.sdk.schemas.annotations.SchemaCreate;
import org.joda.time.Instant;

@DefaultSchema(AutoValueSchema.class)
@AutoValue
public abstract class Result {
  public abstract String getGid();
  public abstract Logic getLogic();
  public abstract Instant getDetectedAt();
  public abstract Instant getCorrectedDetectedAt();

  @SchemaCreate
  public static Result create(String gid, Logic logic, Instant detectedAt) {
    Instant correctedDetectedAt = detectedAt.minus(Config.timeSkew);
    return new AutoValue_Result(gid, logic, detectedAt, correctedDetectedAt);
  }
}
