package eu.tuxtown.crocus.api.attribute;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Logic to serialize and deserialize attribute values to readable form.
 */
@NotNullByDefault
public interface AttributeAdapter<T> {

    /**
     * Stores an attribute value.
     */
    Value<?> store(T attributeValue);

    /**
     * Loads an attribute value. Can return {@code null} to indicate was an error parsing the value.
     */
    @Nullable T load(Value<?> storedValue);

    /**
     * Contains a serialized value used by an attribute adapter.
     * The methods to retrieve values will coerce the actual value to the desired type.
     */
    @NotNullByDefault
    sealed interface Value<T> permits NullValue, BooleanValue, StringValue, IntegralValue, DecimalValue, PackedValue {
        @Nullable T value();
        String stringValue();
        boolean booleanValue();
        BigInteger integralValue();
        BigDecimal decimalValue();

        default int intValue() {
            return this.integralValue().intValue();
        }
        default long longValue() {
            return this.integralValue().longValue();
        }
        default float floatValue() {
            return this.decimalValue().floatValue();
        }
        default double doubleValue() {
            return this.decimalValue().doubleValue();
        }
        default List<Value<?>> unpack() {
            return List.of(this);
        }
    }

    @NotNullByDefault
    enum NullValue implements Value<Void> {
        INSTANCE;

        @Override
        public Void value() {
            return null;
        }

        @Override
        public String stringValue() {
            return "";
        }

        @Override
        public boolean booleanValue() {
            return false;
        }

        @Override
        public BigInteger integralValue() {
            return BigInteger.ZERO;
        }

        @Override
        public BigDecimal decimalValue() {
            return BigDecimal.ZERO;
        }
    }

    @NotNullByDefault
    record StringValue(String value) implements Value<String> {

        @Override
        public String stringValue() {
            return this.value();
        }

        @Override
        public boolean booleanValue() {
            return "true".equalsIgnoreCase(this.value) || "1".equals(this.value);
        }

        @Override
        public BigInteger integralValue() {
            try {
                return new BigInteger(this.stringValue());
            } catch (NumberFormatException e) {
                return this.decimalValue().toBigInteger();
            }
        }

        @Override
        public BigDecimal decimalValue() {
            try {
                return new BigDecimal(this.stringValue());
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
    }

    @NotNullByDefault
    record IntegralValue(BigInteger value) implements Value<BigInteger> {

        public IntegralValue(long value) {
            this(BigInteger.valueOf(value));
        }

        @Override
        public String stringValue() {
            return this.value().toString();
        }

        @Override
        public boolean booleanValue() {
            return this.value().compareTo(BigInteger.ZERO) != 0;
        }

        @Override
        public BigInteger integralValue() {
            return this.value();
        }

        @Override
        public BigDecimal decimalValue() {
            return new BigDecimal(this.value()).stripTrailingZeros();
        }
    }

    @NotNullByDefault
    record DecimalValue(BigDecimal value) implements Value<BigDecimal> {

        public DecimalValue(double value) {
            this(new BigDecimal(Double.toString(value)));
        }

        @Override
        public String stringValue() {
            return this.value().stripTrailingZeros().toString();
        }

        @Override
        public boolean booleanValue() {
            return this.value().compareTo(BigDecimal.ZERO) != 0;
        }

        @Override
        public BigInteger integralValue() {
            return this.value().toBigInteger();
        }

        @Override
        public BigDecimal decimalValue() {
            return this.value();
        }
    }

    @NotNullByDefault
    record BooleanValue(boolean booleanValue) implements Value<Boolean> {

        @Override
        public Boolean value() {
            return this.booleanValue();
        }

        @Override
        public String stringValue() {
            return Boolean.toString(this.booleanValue());
        }

        @Override
        public BigInteger integralValue() {
            return this.booleanValue() ? BigInteger.ONE : BigInteger.ZERO;
        }

        @Override
        public BigDecimal decimalValue() {
            return this.booleanValue() ? BigDecimal.ONE : BigDecimal.ZERO;
        }
    }

    @NotNullByDefault
    record PackedValue(List<Value<?>> value) implements Value<List<Value<?>>> {

        @Override
        public String stringValue() {
            return this.value().stream().map(Value::stringValue).collect(Collectors.joining(",", "[", "]"));
        }

        @Override
        public boolean booleanValue() {
            return !this.value().isEmpty() && this.value().getFirst().booleanValue();
        }

        @Override
        public BigInteger integralValue() {
            return this.value().isEmpty() ? BigInteger.ZERO : this.value().getFirst().integralValue();
        }

        @Override
        public BigDecimal decimalValue() {
            return this.value().isEmpty() ? BigDecimal.ZERO : this.value().getFirst().decimalValue();
        }

        @Override
        public List<Value<?>> unpack() {
            return this.value();
        }
    }
}
