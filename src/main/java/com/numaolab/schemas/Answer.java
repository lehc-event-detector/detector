package com.numaolab.schemas;

import java.io.Serializable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.numaolab.Config;
import com.numaolab.enums.Logic;

import org.apache.beam.sdk.schemas.AutoValueSchema;
import org.apache.beam.sdk.schemas.annotations.DefaultSchema;
import org.apache.beam.sdk.schemas.annotations.SchemaCreate;
import org.joda.time.Instant;

// public class Answer implements Serializable {
//   private  String gid;
//   private  Logic logic;
//   private  Instant start;
//   private  Instant end;

//   public String getGid() {
//     return this.gid;
//   }

//   public Logic getLogic() {
//     return this.logic;
//   }

//   public Instant getStart() {
//     return this.start;
//   }

//   public Instant getEnd() {
//     return this.end;
//   }

//   public Answer(String gid, Logic logic, Instant start, Instant end) {
//     this.gid = gid;
//     this.logic = logic;
//     this.start = start;
//     this.end = end;
//   }

//   public static Answer fromJson(String json) {
//     System.out.println(json);
//     Gson gson = new Gson();
//     return gson.fromJson(json, Answer.class);
//   }
// }

@DefaultSchema(AutoValueSchema.class)
@AutoValue
public abstract class Answer implements Serializable {
  public abstract String getGid();
  public abstract Logic getLogic();
  public abstract Instant getStart();
  public abstract Instant getEnd();

  class JsonAnswer {
    String gid;
    String eri;
    String logic;
    String start;
    String end;
    public JsonAnswer(String gid, String eri, String logic, String start, String end) {
      this.gid = gid;
      this.eri = eri;
      this.logic = logic;
      this.start = start;
      this.end = end;
    }
  }

  public static Answer fromJson(String json) {
    Gson gson = new Gson();
    JsonAnswer ja = gson.fromJson(json, JsonAnswer.class);
    return Answer.create(ja.gid, Config.getLogic(ja.logic, ja.eri), new Instant(Long.parseLong(ja.start)), new Instant(Long.parseLong(ja.end)));
  }

  @SchemaCreate
  public static Answer create(String gid, Logic logic, Instant start, Instant end) {
    return new AutoValue_Answer(gid, logic, start, end);
  }
}