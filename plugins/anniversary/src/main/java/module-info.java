import eu.tuxtown.crocus.anniversary.AnniversarySourceType;
import eu.tuxtown.crocus.api.service.EventSourceType;

module tuxtown.crocus.anniversary {
    requires tuxtown.crocus.core;
    requires org.jetbrains.annotations;

    provides EventSourceType with AnniversarySourceType;
}
