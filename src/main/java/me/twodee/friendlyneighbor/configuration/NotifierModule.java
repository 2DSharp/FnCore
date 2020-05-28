package me.twodee.friendlyneighbor.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import me.twodee.friendlyneighbor.component.FnCoreConfig;

import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.IOException;

public class NotifierModule extends AbstractModule {

    private final FnCoreConfig config;

    public NotifierModule(FnCoreConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {

    }

    @Provides
    @Singleton
    FirebaseMessaging provideFirebaseMessaging() throws IOException {

        FileInputStream serviceAccount =
                new FileInputStream(config.getFirebaseServiceKeyFile());

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl(config.getFirebaseDbUrl())
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);
        return FirebaseMessaging.getInstance(app);
    }
}
