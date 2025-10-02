package eu.tuxtown.crocus.impl.ssl;

import eu.tuxtown.crocus.api.resource.Resource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class SSLContextHelper {

    private static final byte[] PEM_MARKER = new byte[] { '-', '-', '-', '-', '-', 'B', 'E', 'G', 'I', 'N' };

    public static SSLContext load(List<Resource> resources) throws IOException {
        try {
            TrustManagerFactory tmf = loadTrustManagerFactory(resources);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            return sslContext;
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to build ssl context.", e);
        }
    }

    private static TrustManagerFactory loadTrustManagerFactory(List<Resource> resources) throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        List<Certificate> certificates = readCertificates(resources).toList();
        for (int i = 0; i < certificates.size(); i++) {
            ks.setCertificateEntry("cert" + i, certificates.get(i));
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X.509");
        tmf.init(ks);
        return tmf;
    }

    private static Stream<Certificate> readCertificates(List<Resource> resources) throws GeneralSecurityException, IOException {
        List<Stream<Certificate>> certificates = new ArrayList<>(resources.size());
        for (Resource resource : resources) {
            certificates.add(readCertificates(resource));
        }
        return certificates.stream().flatMap(Function.identity());
    }

    private static Stream<Certificate> readCertificates(Resource resource) throws GeneralSecurityException, IOException {
        byte[] data;
        try (InputStream in = resource.openStream()) {
            data = in.readAllBytes();
        }

        boolean isPemFile = true;
        for (int i = 0; i < PEM_MARKER.length; i++) {
            if (i >= data.length || data[i] != PEM_MARKER[i]) {
                isPemFile = false;
                break;
            }
        }

        if (isPemFile) {
            return readPemCertificates(data);
        } else {
            return readDerCertificates(data);
        }
    }

    private static Stream<Certificate> readPemCertificates(byte[] data) throws GeneralSecurityException, IOException {
        String pem =  new String(data, StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .replace("\r", "\n");
        int offset = 0;
        List<Stream<Certificate>> certificates = new ArrayList<>();
        while (true) {
            int beginIdx = pem.indexOf("-----BEGIN", offset);
            if (beginIdx < 0) break;
            int startIdx = pem.indexOf('\n', beginIdx);
            if (startIdx < 0) break;
            startIdx += 1;
            int endIdx = pem.indexOf("-----END", startIdx);
            if (endIdx < 0) break;

            offset = endIdx;
            byte[] der = Base64.getDecoder().decode(pem.substring(startIdx, endIdx).replace("\n", ""));
            certificates.add(readDerCertificates(der));
        }
        return certificates.stream().flatMap(Function.identity());
    }

    private static Stream<Certificate> readDerCertificates(byte[] data) throws GeneralSecurityException, IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            return Stream.of(CertificateFactory.getInstance("X.509").generateCertificate(in));
        }
    }
}
