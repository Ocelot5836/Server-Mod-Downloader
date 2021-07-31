package io.github.ocelot.serverdownloader.server;

import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * @author Ocelot
 */
public class ServerHttpsLoader
{
    private static final Path KEYSTORE_FOLDER = Paths.get("https");

    /**
     * Loads the SSL context from file.
     *
     * @return The context or <code>null</code> if it is not configured
     * @throws IOException              If an error occurs when reading the files
     * @throws GeneralSecurityException If any error happens when trying to establish the SSL context
     */
    @Nullable
    public static SSLContext load() throws IOException, GeneralSecurityException
    {
        if (!Files.exists(KEYSTORE_FOLDER))
            Files.createDirectories(KEYSTORE_FOLDER);

        Path infoFile = KEYSTORE_FOLDER.resolve("README.txt");
        if (!Files.exists(infoFile))
        {
            Files.createFile(infoFile);
            try (FileOutputStream os = new FileOutputStream(infoFile.toFile()))
            {
                IOUtils.write("This folder allows you to enable HTTPS on the downloading server.\n\n============\nINSTRUCTIONS\n============\nPlace the keystore file in this folder named 'downloader.keystore'.\nMake sure the keystore is formatted to use JKS. An example of how to properly set up the keystore can be found here: https://maxrohde.com/2013/09/07/setting-up-ssl-with-netty/\nIn the generated text document, 'password.txt', put in the password for the keystore.\nThe server should start up and print in console how a secure HTTPS server was opened instead of the usual insecure HTTP.", os, StandardCharsets.UTF_8);
            }
        }

        Path keystorePath = KEYSTORE_FOLDER.resolve("downloader.keystore");
        Path keystorePassword = KEYSTORE_FOLDER.resolve("password.txt");

        if (!Files.exists(keystorePassword))
            Files.createFile(keystorePassword);
        if (!Files.exists(keystorePath))
            return null;

        try (InputStream keystoreStream = new FileInputStream(keystorePath.toFile()); InputStream keystorePasswordStream = new FileInputStream(keystorePassword.toFile()))
        {
            char[] password = IOUtils.toCharArray(keystorePasswordStream, StandardCharsets.UTF_8);
            SSLContext serverContext = SSLContext.getInstance("TLS");
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(keystoreStream, password);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("RSA-256");
            kmf.init(ks, password);
            serverContext.init(kmf.getKeyManagers(), null, null);
            return serverContext;
        }
    }
}
