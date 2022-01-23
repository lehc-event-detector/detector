package com.numaolab.transforms;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.AfterWatermark;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.IntervalWindow;
import org.apache.beam.sdk.transforms.windowing.Repeatedly;
import org.apache.beam.sdk.transforms.windowing.SlidingWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.joda.time.Duration;
import org.joda.time.Instant;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.numaolab.Config;
import com.numaolab.enums.Logic;
import com.numaolab.logics.Cmd;
import com.numaolab.schemas.Result;
import com.numaolab.schemas.TagData;
import com.numaolab.schemas.Tags;

public class EventDetector extends PTransform<PCollection<TagData>, PCollectionList<Result>> {
  protected static final Duration windowSize = Config.windowSize;
  protected static final Duration windowEvery = Config.windowEvery;
  protected static final Duration timeSkew = Config.timeSkew;

  protected static class MapGidKey extends DoFn<TagData, KV<String, TagData>> {
    @ProcessElement
    public void process(@Element TagData t, OutputReceiver<KV<String, TagData>> out, IntervalWindow w) {
      out.output(KV.of(t.getGid(), t));
    }
  }

  protected static class DetectDROP extends DoFn<KV<String, Iterable<TagData>>, Result> {
    public Boolean detect(Tags prev, Tags curr) {
      return Cmd.detectDrop(prev, curr);
    }
    @ProcessElement
    public void process(ProcessContext ctx, IntervalWindow w) {

      // この処理はGroupByでやったほうがいい
      Instant middle = w.start().plus(windowEvery);
      List<TagData> prevTags = new ArrayList<>();
      List<TagData> currTags = new ArrayList<>();
      for (TagData t: ctx.element().getValue()) {
        Instant time = Instant.ofEpochMilli(Long.parseLong(t.getTime())/1000).plus(timeSkew);
        if (time.isAfter(middle)) {
          currTags.add(t);
        } else {
          prevTags.add(t);
        }
      }

      Tags prev = new Tags((Collection<TagData>) prevTags);
      Tags curr = new Tags((Collection<TagData>) currTags);

      if (this.detect(prev, curr)) {
        ctx.output(Result.create(curr.gid, curr.logic, ctx.timestamp()));
      }
    }
  }

  protected static class DetectEMERGE extends DetectDROP {
    @Override
    public Boolean detect(Tags prev, Tags curr) {
      return Cmd.detectEmerge(prev, curr);
    }
  }

  protected static class DetectCROSS extends DoFn<KV<String, Iterable<TagData>>, Result> {
    public Boolean detect(Tags prev, Tags curr) {
      return Cmd.detectCross(prev, curr);
    }
    @ProcessElement
    public void process(ProcessContext ctx, IntervalWindow w) {
      Jedis jedis = new Jedis("redis", 6379);
      try {
        Tags curr = new Tags((Collection<TagData>) ctx.element().getValue());
        // 非重要タグと重要タグがそれぞれ1枚以上必要
        if (curr.niTags.size() < 1 || curr.yiTags.size() < 1) { new Exception(); }
        // キャッシュがあれば検出実行
        String cache = jedis.get(curr.gid);
        if (cache != null) {
          Tags prev = Tags.fromJson(cache);
          if (this.detect(prev, curr)) {
            ctx.output(Result.create(curr.gid, curr.logic, ctx.timestamp()));
          }
        }
        // キャッシュを更新
        jedis.set(curr.gid, curr.toJson());
      } catch (Exception e) {
      } finally {
        jedis.close();
      }
    }
  }

  protected static class DetectMERGE extends DetectCROSS {
    @Override
    public Boolean detect(Tags prev, Tags curr) {
      return Cmd.detectMerge(prev, curr);
    }
  }

  protected static class DetectDIVIDE extends DetectCROSS {
    @Override
    public Boolean detect(Tags prev, Tags curr) {
      return Cmd.detectDivide(prev, curr);
    }
  }

  // protected static class AsyncDetectCROSS extends JavaAsyncDoFn<KV<String, Iterable<TagData>>, Result, RedisAsyncCommands<String, String>> {
  //   @Override
  //   public CompletableFuture<Result> processElement(KV<String, Iterable<TagData>> input) {
  //     RedisAsyncCommands<String, String> commands = getResource();
  //     Tags curr = new Tags((Collection<TagData>) input.getValue());

  //     final RedisFuture<String> future = commands.get(curr.gid);
  //     return future.thenApply((new Function<String, Result>() {
  //       @Override
  //       public Result apply(String cache) {
  //         Result res = Result.create(curr.gid, curr.logic, Instant.now());
  //         // 非重要タグと重要タグがそれぞれ1枚以上必要
  //         if (curr.niTags.size() < 1 || curr.yiTags.size() < 1) {

  //         } else {
  //           if (cache != null) {
  //             Tags prev = Tags.fromJson(cache);
  //             if (
  //               (curr.logic == Logic.CROSS && Cmd.detectCross(prev, curr)) ||
  //               (curr.logic == Logic.MERGE && Cmd.detectMerge(prev, curr)) ||
  //               (curr.logic == Logic.DIVIDE && Cmd.detectDivide(prev, curr))
  //             ) {
  //               res = Result.create(curr.gid, curr.logic, Instant.now());
  //             }
  //           }
  //           // キャッシュを更新
  //           commands.set(curr.gid, curr.toJson());
  //         }

  //         return res;
  //       }
  //     })).toCompletableFuture();
  //   }

  //   @Override
  //   public DoFnWithResource.ResourceType getResourceType() {
  //       return DoFnWithResource.ResourceType.PER_CLONE;
  //   }

  //   @Override
  //   public RedisAsyncCommands<String, String> createResource() {
  //     RedisClient redisClient = RedisClient.create("redis://redis:6379");
  //     StatefulRedisConnection<String, String> connection = redisClient.connect();
  //     RedisAsyncCommands<String, String> commands = connection.async();
  //     return commands;
  //   }
  // }

  @Override
  public PCollectionList<Result> expand(PCollection<TagData> tagDataRows) {

    return PCollectionList.of(Config.logicMap.values().stream().map(l -> {
        /**
         * **********************************************************************************************
         * Filter by Logic
         * **********************************************************************************************
         */
        PCollection<TagData> filteredData = tagDataRows.apply(
          ParDo.of(
            new DoFn<TagData, TagData>() {
              @ProcessElement
              public void processElement(ProcessContext ctx) {
                if (l == Config.getLogic(ctx.element().getLogic(), ctx.element().getEri())) {
                  ctx.output(ctx.element());
                }
              }
            }
          )
        );

        /**
         * **********************************************************************************************
         * Windowing
         * **********************************************************************************************
         */
        PCollection<TagData> windowingData;
        if (l == Logic.DROP || l == Logic.EMERGE) {
          windowingData =
            filteredData.apply(
              "Sliding Windowing",
              Window.<TagData>into(
                SlidingWindows.of(windowSize).every(windowEvery)));
        } else {
          windowingData =
            filteredData.apply(
              "Fixed Windowing",
              Window.<TagData>into(
                FixedWindows.of(windowEvery)
              ).triggering(Repeatedly.forever(AfterWatermark.pastEndOfWindow())).withAllowedLateness(Duration.ZERO).discardingFiredPanes());
        }

        /**
         * **********************************************************************************************
         * Map <GID, TagData>
         * **********************************************************************************************
         */
        PCollection<KV<String, TagData>> kvData =
            windowingData.apply("Map", ParDo.of(new MapGidKey()));

        /**
         * **********************************************************************************************
         * GroupByGID <GID, Iterable<TagData>>
         * **********************************************************************************************
         */
        PCollection<KV<String, Iterable<TagData>>> groupData =
            kvData.apply("GroupByGID", GroupByKey.create());

        /**
         * **********************************************************************************************
         * Detect Logic per Group
         * **********************************************************************************************
         */
        PCollection<Result> result;
        if (l == Logic.DROP) {
          result = groupData.apply("Detect DROP per Group", ParDo.of(new DetectDROP()));
        } else if (l == Logic.EMERGE) {
          result = groupData.apply("Detect DROP per Group", ParDo.of(new DetectEMERGE()));
        } else if (l == Logic.CROSS) {
          result = groupData.apply("Detect DROP per Group", ParDo.of(new DetectCROSS()));
        } else if (l == Logic.MERGE) {
          result = groupData.apply("Detect DROP per Group", ParDo.of(new DetectMERGE()));
        } else {
          result = groupData.apply("Detect DROP per Group", ParDo.of(new DetectDIVIDE()));
        }

        return result;
    }).collect(Collectors.toList()));
  }
}
