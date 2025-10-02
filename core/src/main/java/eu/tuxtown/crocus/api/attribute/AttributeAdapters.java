package eu.tuxtown.crocus.api.attribute;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@NotNullByDefault
public class AttributeAdapters {

    public static final AttributeAdapter<String> STRING = new AttributeAdapter<>() {

        @Override
        public Value<?> store(String attributeValue) {
            return new StringValue(attributeValue);
        }

        @Override
        public String load(Value<?> storedValue) {
            return storedValue.stringValue();
        }
    };

    public static final AttributeAdapter<Boolean> BOOLEAN = new AttributeAdapter<>() {

        @Override
        public Value<?> store(Boolean attributeValue) {
            return new BooleanValue(attributeValue);
        }

        @Override
        public Boolean load(Value<?> storedValue) {
            return storedValue.booleanValue();
        }
    };

    public static final AttributeAdapter<Integer> INT = new AttributeAdapter<>() {

        @Override
        public Value<?> store(Integer attributeValue) {
            return new IntegralValue(attributeValue);
        }

        @Override
        public Integer load(Value<?> storedValue) {
            return storedValue.intValue();
        }
    };

    public static final AttributeAdapter<ZoneId> TIMEZONE = new AttributeAdapter<>() {

        @Override
        public Value<?> store(ZoneId attributeValue) {
            return new StringValue(attributeValue.getId());
        }

        @Override
        public ZoneId load(Value<?> storedValue) {
            try {
                return ZoneId.of(storedValue.stringValue());
            } catch (DateTimeException e) {
                return ZoneOffset.UTC;
            }
        }
    };

    public static final AttributeAdapter<URI> URI = new AttributeAdapter<>() {

        @Override
        public Value<?> store(URI attributeValue) {
            return new StringValue(attributeValue.toString());
        }

        @Override
        public @Nullable URI load(Value<?> storedValue) {
            try {
                return new URI(storedValue.stringValue());
            } catch (URISyntaxException e) {
                return null;
            }
        }
    };

    public static <T> AttributeAdapter<List<T>> list(Class<T> cls, AttributeAdapter<T> adapter) {
        return new AttributeAdapter<>() {

            @Override
            public Value<?> store(List<T> attributeValue) {
                List<? extends Value<?>> list = attributeValue.stream().map(adapter::store).toList();
                //noinspection unchecked
                return new PackedValue((List<Value<?>>) list);
            }

            @Override
            public List<T> load(Value<?> storedValue) {
                //noinspection NullableProblems
                return storedValue.unpack().stream().map(adapter::load).filter(Objects::nonNull).toList();
            }
        };
    }
}
