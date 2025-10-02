import eu.tuxtown.crocus.api.service.EventFilterType;
import eu.tuxtown.crocus.filter.ModifyFilterType;
import eu.tuxtown.crocus.filter.SimpleFilterType;

module tuxtown.crocus.filter {
    requires static org.jetbrains.annotations;

    requires tuxtown.crocus.core;

    provides EventFilterType with SimpleFilterType, ModifyFilterType;
}
