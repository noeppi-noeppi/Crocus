import eu.tuxtown.crocus.api.service.EventSourceType;
import eu.tuxtown.crocus.csv.CsvSourceType;

module tuxtown.crocus.csv {
    requires tuxtown.crocus.core;
    requires static org.jetbrains.annotations;

    requires de.siegmar.fastcsv;

    provides EventSourceType with CsvSourceType;
}
