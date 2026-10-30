package eu.tuxtown.crocus.caldav.util;

import com.github.sardine.model.Multistatus;
import com.github.sardine.model.Propstat;
import com.github.sardine.model.Response;
import com.github.sardine.report.SardineReport;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NotNullByDefault
public class ListEventsReport extends SardineReport<List<URI>> {

    private final URI uri;

    public ListEventsReport(URI uri) {
        this.uri = ResourceHelper.normalizeURI(uri);
    }

    @Override
    public String toXml() {
        return """
               <?xml version="1.0" encoding="UTF-8"?>
               <c:calendar-query
                   xmlns:d="DAV:"
                   xmlns:c="urn:ietf:params:xml:ns:caldav">
                   <d:prop>
                       <d:getetag/>
                       <c:calendar-data/>
                   </d:prop>
                   <c:filter>
                       <c:comp-filter name="VCALENDAR">
                           <c:comp-filter name="VEVENT"/>
                       </c:comp-filter>
                   </c:filter>
               </c:calendar-query>
               """;
    }

    @Override
    public @Nullable Object toJaxb() {
        return null;
    }

    @Override
    public List<URI> fromMultistatus(Multistatus multistatus) {
        if (multistatus.getResponse() == null) return List.of();
        List<URI> resources = new ArrayList<>(multistatus.getResponse().size());
        for (Response result : multistatus.getResponse()) {
            if (result.getStatus() != null && this.isError(result.getStatus())) {
                throw new UncheckedIOException(new IOException("Error retrieving events: " + result.getStatus()));
            }
            if (result.getPropstat() != null) for (Propstat ps : result.getPropstat()) {
                if (ps.getStatus() != null && this.isError(ps.getStatus())) {
                    throw new UncheckedIOException(new IOException("Error retrieving events: " + ps.getStatus()));
                }
            }
            if (result.getHref() == null || result.getHref().isEmpty()) {
                throw new UncheckedIOException(new IOException("Calendar resource without href"));
            }
            for (String href : result.getHref()) {
                resources.add(this.uri.resolve(href));
            }
        }
        return Collections.unmodifiableList(resources);
    }

    private boolean isError(String statusLine) {
        String[] parts = statusLine.split(" ");
        if (parts.length < 2) return true;
        try {
            return Integer.parseInt(parts[1]) / 100 != 2;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}
