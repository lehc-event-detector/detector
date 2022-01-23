package com.numaolab.schemas;


import org.joda.time.Instant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.numaolab.enums.Logic;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.numaolab.Config;

public class Tags {
  public final Logic logic;
  public final int k;
  public final int kd;
  public final String gid;
  public final String other;
  public final List<TagData> yiTags;
  public final List<TagData> niTags;

  public Tags(Collection<TagData> tags) {
    TagData anyTag = tags.stream().findAny().get();
    this.logic = Config.getLogic(anyTag.getLogic(), anyTag.getEri());
    this.k = Integer.parseInt(anyTag.getK(), 2);
    this.kd = Integer.parseInt(anyTag.getKd(), 2);
    this.gid = anyTag.getGid();
    this.other = anyTag.getOther();
    // 最新のものだけにフィルターする
    HashMap<String, TagData> yiCache = new HashMap<>();
    HashMap<String, TagData> niCache = new HashMap<>();
    for (TagData t: tags) {
      String igs = t.getIgs();
      Instant time = Instant.ofEpochMilli(Long.parseLong(t.getTime())/1000);
      Boolean mbit = t.getMbit().equals("1");
      if (mbit) {
        if (!yiCache.containsKey(igs) || time.isAfter(Instant.ofEpochMilli(Long.parseLong(yiCache.get(igs).getTime())/1000))) {
          yiCache.put(igs, t);
        }
      } else {
        if (!niCache.containsKey(igs) || time.isAfter(Instant.ofEpochMilli(Long.parseLong(niCache.get(igs).getTime())/1000))) {
          niCache.put(igs, t);
        }
      }
    }
    this.yiTags = new ArrayList<TagData>(yiCache.values());
    this.niTags = new ArrayList<TagData>(niCache.values());
  }

  public static Tags fromJson(String json) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      TagData[] tags = mapper.readValue(json, TagData[].class);
      return new Tags(Arrays.asList(tags));
    } catch (IOException e) {
      System.err.println(e);
      return new Tags(new ArrayList<>());
    }
  }

  public String toJson() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(Stream.concat(this.niTags.stream(), this.yiTags.stream()).collect(Collectors.toList()));
      return json;
    } catch (IOException e) {
      System.err.println(e);
      return "";
    }
  }
}