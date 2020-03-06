package org.eclipse.vorto.codegen.webofthings;

import org.json.JSONObject;
import org.mozilla.iot.webthing.Property;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.Value;

public class FunctionBlockProperty extends Property<JSONObject> {

  public FunctionBlockProperty(Thing thing, String name,
      Value<JSONObject> value) {
    super(thing, name, value);
  }

}
