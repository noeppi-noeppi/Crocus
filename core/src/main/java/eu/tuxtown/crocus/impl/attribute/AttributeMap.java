package eu.tuxtown.crocus.impl.attribute;

import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.attribute.AttributeAdapter;
import eu.tuxtown.crocus.api.attribute.Attributes;
import eu.tuxtown.crocus.impl.dataio.ValueIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// externalizable wrapper around an attribute map that serializes the attributes using their adapter.
@NotNullByDefault
public class AttributeMap implements Externalizable {

    @Serial
    private static final long serialVersionUID = 0;

    private Map<Attribute<?>, ?> attributes;

    public AttributeMap() {
        this.attributes = Map.of();
    }

    public AttributeMap(Map<Attribute<?>, ?> attributes) {
        this.attributes = Map.copyOf(attributes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeExternal(ObjectOutput out) throws IOException {
        Map<Attribute<?>, ?> map = this.attributes;
        out.writeInt(map.size());
        for (Attribute<?> attribute : map.keySet().stream().sorted().toList()) {
            out.writeUTF(attribute.name());
            ValueIO.writeValue(out, ((AttributeAdapter<@NotNull Object>) attribute.adapter()).store(map.get(attribute)));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        Map<Attribute<?>, Object> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String attributeName = in.readUTF();
            Attribute<?> attribute = Attributes.get(attributeName).orElse(null);
            if (attribute == null) throw new IOException("Unknown attribute in serialized data: " + attributeName);
            Object value = ((AttributeAdapter<@NotNull Object>) attribute.adapter()).load(ValueIO.readValue(in));
            if (value == null) continue;
            map.put(attribute, value);
        }
        this.attributes = Map.copyOf(map);
    }

    public Map<Attribute<?>, ?> map() {
        return this.attributes;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Attribute<T> attribute) {
        return Optional.ofNullable((T) this.attributes.get(attribute));
    }
}
