import eu.tuxtown.crocus.api.service.AttributeProvider;
import eu.tuxtown.crocus.api.service.EventSourceType;
import eu.tuxtown.crocus.frab.FrabSourceType;
import eu.tuxtown.crocus.frab.api.FrabAttributes;

module tuxtown.crocus.frab {
    requires static org.jetbrains.annotations;

    requires tuxtown.crocus.core;
    requires jakarta.xml.bind;
    requires org.glassfish.jaxb.runtime;

    exports eu.tuxtown.crocus.frab.api;
    opens eu.tuxtown.crocus.frab.bind to jakarta.xml.bind;
    opens eu.tuxtown.crocus.frab.model to jakarta.xml.bind;

    provides AttributeProvider with FrabAttributes;
    provides EventSourceType with FrabSourceType;
}
