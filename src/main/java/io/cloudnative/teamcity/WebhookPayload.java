package io.cloudnative.teamcity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import java.util.*;

/**
 * https://cloudnative.io/docs/bakery/json-webhook/
 */
@AllArgsConstructor(staticName = "of")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhookPayload {

  String title;
  String summary;
  String themeColor;
  List<Section> sections;
  
  @Builder
  static class Section{
    String title;
    List<Fact> facts;
  }
  
  @Builder
  static class Fact {
    String name;
    String value;
  }

}
