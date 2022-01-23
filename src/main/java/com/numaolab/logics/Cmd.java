package com.numaolab.logics;

import java.util.List;

import com.numaolab.schemas.TagData;
import com.numaolab.schemas.Tags;

public class Cmd {
  private static Double rssiAve(List<TagData> tags) {
    return tags.parallelStream().mapToDouble(t -> Float.parseFloat(t.getRssi())).average().orElse(-999);
  }

  public static Boolean detectCross(Tags prev, Tags curr) {
    double prevYI = rssiAve(prev.yiTags);
    double prevNI = rssiAve(prev.niTags);
    double currYI = rssiAve(curr.yiTags);
    double currNI = rssiAve(curr.niTags);
    return (prevYI < prevNI && currYI > currNI) || (prevYI > prevNI && currYI < currNI);
  }

  public static Boolean detectMerge(Tags prev, Tags curr) {
    double prevDiff = Math.abs(rssiAve(prev.yiTags) - rssiAve(prev.niTags));
    double currDiff = Math.abs(rssiAve(curr.yiTags) - rssiAve(curr.niTags));
    int threshold = Integer.parseInt(prev.other, 2);
    return prevDiff > threshold && currDiff <= threshold;
  }

  public static Boolean detectDivide(Tags prev, Tags curr) {
    double prevDiff = Math.abs(rssiAve(prev.yiTags) - rssiAve(prev.niTags));
    double currDiff = Math.abs(rssiAve(curr.yiTags) - rssiAve(curr.niTags));
    int threshold = Integer.parseInt(prev.other, 2);
    return prevDiff < threshold && currDiff >= threshold;
  }

  public static Boolean detectDrop(Tags prev, Tags curr) {
    return (prev.niTags.size() + prev.yiTags.size()) > 0 && (curr.niTags.size() + curr.yiTags.size()) == 0;
  }

  public static Boolean detectEmerge(Tags prev, Tags curr) {
    return (prev.niTags.size() + prev.yiTags.size()) < prev.k && (curr.niTags.size() + curr.yiTags.size()) == curr.k;
  }
}
