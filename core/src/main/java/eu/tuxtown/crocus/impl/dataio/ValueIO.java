package eu.tuxtown.crocus.impl.dataio;

import eu.tuxtown.crocus.api.attribute.AttributeAdapter;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ValueIO {

    private static final BigInteger INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    public static void writeValue(ObjectOutput out, AttributeAdapter.Value<?> value) throws IOException {
        switch (value) {
            case AttributeAdapter.NullValue nullValue -> out.writeByte(0xFF);
            case AttributeAdapter.BooleanValue booleanValue -> out.writeByte(booleanValue.value() ? 1 : 0);
            case AttributeAdapter.IntegralValue integralValue -> {
                if (integralValue.value().compareTo(INT_MIN) >= 0 && integralValue.value().compareTo(INT_MAX) <= 0) {
                    out.writeByte(2);
                    out.writeInt(integralValue.value().intValue());
                } else if (integralValue.value().compareTo(LONG_MIN) >= 0 && integralValue.value().compareTo(LONG_MAX) <= 0) {
                    out.writeByte(3);
                    out.writeLong(integralValue.value().longValue());
                } else {
                    out.writeByte(4);
                    out.writeObject((BigInteger) integralValue.value());
                }
            }
            case AttributeAdapter.DecimalValue decimalValue -> {
                out.writeByte(5);
                out.writeObject((BigDecimal) decimalValue.value());
            }
            case AttributeAdapter.StringValue stringValue -> {
                out.writeByte(6);
                out.writeUTF(stringValue.value());
            }
            case AttributeAdapter.PackedValue packedValue -> {
                out.writeByte(0xFE);
                out.writeInt(packedValue.value().size());
                for (AttributeAdapter.Value<?> member : packedValue.value()) {
                    writeValue(out, member);
                }
            }
        }
    }

    public static AttributeAdapter.Value<?> readValue(ObjectInput in) throws IOException, ClassNotFoundException {
        try {
            int typ = in.readUnsignedByte();
            return switch (typ) {
                case 0xFF -> AttributeAdapter.NullValue.INSTANCE;
                case 0 -> new AttributeAdapter.BooleanValue(false);
                case 1 -> new AttributeAdapter.BooleanValue(true);
                case 2 -> new AttributeAdapter.IntegralValue(in.readInt());
                case 3 -> new AttributeAdapter.IntegralValue(in.readLong());
                case 4 -> new AttributeAdapter.IntegralValue((BigInteger) in.readObject());
                case 5 -> new AttributeAdapter.DecimalValue((BigDecimal) in.readObject());
                case 6 -> new AttributeAdapter.StringValue(in.readUTF());
                case 0xFE -> {
                    int size = in.readInt();
                    List<AttributeAdapter.Value<?>> members = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) members.add(readValue(in));
                    yield new AttributeAdapter.PackedValue(List.copyOf(members));
                }
                default -> throw new IOException("Invalid serialized value.");
            };
        } catch (ClassCastException e) {
            throw new IOException("Invalid serialized value.", e);
        }
    }
}
