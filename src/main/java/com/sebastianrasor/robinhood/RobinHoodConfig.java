package com.sebastianrasor.robinhood;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.ColorPicker;

@Config(name = "robinhood")
public class RobinHoodConfig implements ConfigData {
  float lineWidth = 3f;
  @ColorPicker()
  int lineColor = 0xFFFFFF;
  boolean useBlending = true;
}
