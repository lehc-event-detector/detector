package com.numaolab.transforms;

import org.apache.beam.sdk.io.mqtt.MqttIO;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;

public class InputFromMqtt extends PTransform<PBegin, PCollection<byte[]>> {
  private final String address;
  private final String topic;

  public InputFromMqtt(String address, String topic) {
    this.address = address;
    this.topic = topic;
  }

  @Override
  public PCollection<byte[]> expand(PBegin input) {
    return
      input.apply(
        MqttIO.read().withConnectionConfiguration(
          MqttIO.ConnectionConfiguration.create(address, topic)));
  }
}
