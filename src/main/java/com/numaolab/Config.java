package com.numaolab;

import java.util.HashMap;
import java.util.Map;

import com.numaolab.enums.Logic;

import org.joda.time.Duration;

public class Config {
  public static final Duration windowSize = Duration.millis(2000);
  public static final Duration windowEvery = Duration.millis(1000);
  public static final Duration timeSkew = Duration.millis(1000);
  public static final Map<String, Logic> logicMap = new HashMap<String, Logic>() {
    {
      put("0000000000000001" + "0000", Logic.EMERGE);
      put("0000000000000010" + "0000", Logic.DROP);
      // put("0000000" + "0001", Logic.DROP_RSSI);
      // put("0000001" + "0001", Logic.EMERGE_RSSI);
      put("0000000000000100" + "0000", Logic.CROSS);
      // put("0000010" + "0001", Logic.CROSS_RSSI);
      put("0000000000001000" + "0001", Logic.DIVIDE);
      put("0000000000010000" + "0001", Logic.MERGE);
    }
  };

  public static final Logic getLogic(String code, String eri) {
    Logic l = logicMap.get(code+eri);
    if (l == null) l = Logic.NONE;
    return l;
  }
}
