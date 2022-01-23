package com.numaolab.schemas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import org.apache.beam.sdk.schemas.AutoValueSchema;
import org.apache.beam.sdk.schemas.annotations.DefaultSchema;
import org.apache.beam.sdk.schemas.annotations.SchemaCreate;

@DefaultSchema(AutoValueSchema.class)
@AutoValue
public abstract class TagData {
  public abstract String getHeader();
  public abstract String getOther();
  public abstract String getEnv();
  public abstract String getEri();
  public abstract String getLogic();
  public abstract String getK();
  public abstract String getKd();
  public abstract String getGid();
  public abstract String getMbit();
  public abstract String getIgs();
  public abstract String getRssi();
  public abstract String getPhase();
  public abstract String getTime();


  @SchemaCreate
  @JsonCreator
  public static TagData create(@JsonProperty("header")String header, @JsonProperty("other")String other, @JsonProperty("env")String env, @JsonProperty("eri")String eri, @JsonProperty("logic")String logic, @JsonProperty("k")String k, @JsonProperty("kd")String kd, @JsonProperty("gid")String gid, @JsonProperty("mbit")String mbit, @JsonProperty("igs")String igs, @JsonProperty("rssi")String rssi, @JsonProperty("phase")String phase, @JsonProperty("time")String time) {
    return new AutoValue_TagData(header, other, env, eri, logic, k, kd, gid, mbit, igs, rssi, phase, time);
  }
}

/*
public String getHeader() {
    return this.header();
  }
  public String getOther() {
    return this.other();
  }
  public String getEnv() {
    return this.env();
  }
  public String getEri() {
    return this.eri();
  }
  public Logic getLogic() {
    return Config.getLogic(this.logic(), this.eri());
  }
  public Integer getK() {
    return Integer.parseInt(k(), 2);
  }
  public Integer getKd() {
    return Integer.parseInt(kd(), 2);
  }
  public String getGid() {
    return gid();
  }
  public Boolean getMbit() {
    return this.mbit().equals("1");
  }
  public String getIgs() {
    return igs();
  }
  public Float getRssi() {
    return Float.parseFloat(rssi());
  }
  public Float getPhase() {
    return Float.parseFloat(phase());
  }
  public Instant getTime() {
    return Instant.ofEpochMilli(Long.parseLong(timestamp()));
  }
*/