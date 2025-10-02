package eu.tuxtown.crocus.core.configuration;

import eu.tuxtown.crocus.api.service.Nameable;
import eu.tuxtown.crocus.core.loader.Services;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public record ConfiguredService<S extends Nameable, T>(Services.Service<S> service, String id, T value) {

    public Services.Key key() {
        return this.service().key();
    }

    public Identifier identifier() {
        return new Identifier(this.key(), this.id());
    }

    public record Identifier(Services.Key key, String id) {

        public String moduleName() {
            return this.key().moduleName();
        }

        public String serviceName() {
            return this.key().serviceName();
        }
    }
}
