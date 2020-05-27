package me.twodee.friendlyneighbor.service;

import com.google.firebase.messaging.*;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class Notifier {

    private final FirebaseMessaging messaging;

    @Inject
    public Notifier(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    public void sendPostRecommendation(List<String> ids) {
        try {
            List<String> registrationTokens = Arrays.asList(
                    "YOUR_REGISTRATION_TOKEN_1",
                    // ...
                    "YOUR_REGISTRATION_TOKEN_n"
            );


            MulticastMessage message = MulticastMessage.builder()
                    .putData("score", "850")
                    .putData("time", "2:45")
                    .setNotification(Notification.builder()
                                             .setTitle("")
                                             .build())
                    .addAllTokens(registrationTokens)
                    .build();
            BatchResponse response = messaging.sendMulticast(message);

            System.out.println(response.getSuccessCount() + " messages were sent successfully");
        } catch (FirebaseMessagingException e) {

        }
    }

    public void sendAll() {
        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                                             .setTitle("Price drop")
                                             .setBody("5% off all electronics")
                                             .build()

                    ).build();
            String response = messaging.send(message);

            System.out.println(response);
        } catch (FirebaseMessagingException e) {

        }

    }
}
