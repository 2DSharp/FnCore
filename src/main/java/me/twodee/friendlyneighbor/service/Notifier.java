package me.twodee.friendlyneighbor.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import me.twodee.friendlyneighbor.entity.MessageRecipient;
import me.twodee.friendlyneighbor.repository.MessagingRepository;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Notifier {

    private final FirebaseMessaging messaging;
    private final MessagingRepository repository;

    @Inject
    public Notifier(FirebaseMessaging messaging, MessagingRepository repository) {
        this.messaging = messaging;
        this.repository = repository;
    }

    public void saveToNotification(String userId, String token) {
        MessageRecipient messageRecipient = new MessageRecipient(userId, token);
        repository.save(messageRecipient);
    }

    public void sendPostRecommendation(List<String> ids) {
        try {

            List<MessageRecipient> recipients = repository.findTokensByIds(ids);
            List<String> tokens = recipients.stream().map(MessageRecipient::getToken).collect(Collectors.toList());

            MulticastMessage message = MulticastMessage.builder()
                    .putData("type", "discover")
                    .setNotification(Notification.builder()
                                             .setBody(
                                                     "There are similar items to your recent search people are looking for, want to have a look?")
                                             .setTitle("Someone just posted a post may fulfil your requirements.")
                                             .build())
                    .addAllTokens(tokens)
                    .build();
            BatchResponse response = messaging.sendMulticast(message);
            log.info(response.getSuccessCount() + " messages were sent successfully");
        } catch (FirebaseMessagingException e) {
            log.error("Error sending notif", e);
        }
    }

    public void sendNewResponseNotification(String id) {
        try {
            String token = repository.findById(id).getToken();

            Message message = Message.builder()
                    .putData("type", "response")
                    .setNotification(Notification.builder()
                                             .setBody("Someone found your post fulfiling their needs, have a look!")
                                             .setTitle("Someone responded to your post!")
                                             .build())
                    .setToken(token)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Notification sent to " + id + ". Response: " + response);
        } catch (FirebaseMessagingException e) {
            log.error("Error sending notif", e);

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
