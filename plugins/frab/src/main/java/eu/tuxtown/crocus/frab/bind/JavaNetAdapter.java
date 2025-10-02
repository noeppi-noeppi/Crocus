package eu.tuxtown.crocus.frab.bind;

import java.net.URI;

public class JavaNetAdapter {
    
    public static URI parseURI(String uri) {
        return URI.create(uri);
    }
    
    public static String printURI(URI uri) {
        return uri.toString();
    }
}
