package eu.tuxtown.crocus.impl.json;

import com.google.gson.*;
import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.attribute.AttributeAdapter;
import eu.tuxtown.crocus.api.attribute.Attributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NotNullByDefault;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NotNullByDefault
public class AttributeJson {

   public static JsonObject attributesToJson(Map<Attribute<?>, ?> attributeMap) {
       JsonObject json = new JsonObject();
       for (Map.Entry<Attribute<?>, ?> entry : attributeMap.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().name())).toList()) {
           //noinspection unchecked
           json.add(entry.getKey().name(), valueToJson(((AttributeAdapter<@NotNull Object>) entry.getKey().adapter()).store(entry.getValue())));
       }
       return json;
   }

   public static Map<Attribute<?>, ?> attributesFromJson(JsonObject json) {
       Map<Attribute<?>, Object> map = new HashMap<>();
       for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
           Attribute<?> attribute = Attributes.get(entry.getKey()).orElse(null);
           if (attribute == null) continue;
           Object value = attribute.adapter().load(valueFromJson(entry.getValue()));
           if (value != null) map.put(attribute, value);
       }
       return Map.copyOf(map);
   }

   public static JsonElement valueToJson(AttributeAdapter.Value<?> value) {
       return switch (value) {
           case AttributeAdapter.NullValue nullValue -> JsonNull.INSTANCE;
           case AttributeAdapter.BooleanValue booleanValue -> new JsonPrimitive(value.booleanValue());
           case AttributeAdapter.IntegralValue integralValue -> new JsonPrimitive(value.integralValue());
           case AttributeAdapter.DecimalValue decimalValue -> new JsonPrimitive(value.decimalValue());
           case AttributeAdapter.StringValue stringValue -> new JsonPrimitive(value.stringValue());
           case AttributeAdapter.PackedValue packedValue -> {
               JsonArray array = new JsonArray();
               for (AttributeAdapter.Value<?> elem : value.unpack()) {
                   array.add(valueToJson(elem));
               }
               yield array;
           }
       };
   }

    public static AttributeAdapter.Value<?> valueFromJson(JsonElement element) {
       if (element instanceof JsonPrimitive value) {
           if (value.isBoolean()) {
               return new AttributeAdapter.BooleanValue(value.getAsBoolean());
           } else if (value.isNumber()) {
               Number number = value.getAsNumber();
               if (number instanceof BigInteger integral) {
                   return new AttributeAdapter.IntegralValue(integral);
               } else if (number instanceof BigDecimal decimal) {
                   return new AttributeAdapter.DecimalValue(decimal.stripTrailingZeros());
               } else if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
                   return new AttributeAdapter.IntegralValue(number.longValue());
               } else {
                   return new AttributeAdapter.DecimalValue(number.doubleValue());
               }
           } else {
               return new AttributeAdapter.StringValue(value.getAsString());
           }
       } else if (element instanceof JsonArray array) {
           List<? extends AttributeAdapter.Value<?>> values = array.asList().stream().map(AttributeJson::valueFromJson).toList();
           //noinspection unchecked
           return new AttributeAdapter.PackedValue((List<AttributeAdapter.Value<?>>) values);
       } else {
           return AttributeAdapter.NullValue.INSTANCE;
       }
   }
}
