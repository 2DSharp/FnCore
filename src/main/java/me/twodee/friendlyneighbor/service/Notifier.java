package me.twodee.friendlyneighbor.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import me.twodee.friendlyneighbor.dto.ResultObject;
import me.twodee.friendlyneighbor.dto.SuccessResult;
import me.twodee.friendlyneighbor.entity.MessageRecipient;
import me.twodee.friendlyneighbor.repository.MessagingRepository;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class Notifier {

    private final FirebaseMessaging messaging;
    private final MessagingRepository repository;

    public enum ResponseType {
        ACCEPT,
        RESPOND
    }

    @Inject
    public Notifier(FirebaseMessaging messaging, MessagingRepository repository) {
        this.messaging = messaging;
        this.repository = repository;
    }

    public ResultObject saveToNotification(String userId, String token) {
        MessageRecipient messageRecipient = new MessageRecipient(userId, token);
        repository.save(messageRecipient);
        return new SuccessResult();
    }

    public ResultObject sendPostRecommendation(List<String> ids) {
        try {

            List<MessageRecipient> recipients = repository.findTokensByIds(ids);
            List<String> tokens = recipients.stream().map(MessageRecipient::getToken).collect(Collectors.toList());

            MulticastMessage message = MulticastMessage.builder()
                    .putData("type", "discover")
                    .putData("title", "Someone just posted a post may fulfil your requirements.")
                    .putData("content",
                             "There are similar items to your recent search people are looking for, want to have a look?")
                    .addAllTokens(tokens)
                    .build();
            BatchResponse response = messaging.sendMulticast(message);
            log.info(response.getSuccessCount() + " messages were sent successfully");
            return new SuccessResult();

        } catch (FirebaseMessagingException e) {
            log.error("Error sending notif", e);
            return new ResultObject("internal", "Error sending notification, try again");
        }
    }

    public ResultObject sendNewResponseNotification(String id, String nameOfRespondingUser, ResponseType responseType) {
        try {
            String token = repository.findById(id).getToken();
            Map<String, String> messageContent = new HashMap<>();
            if (responseType == ResponseType.ACCEPT) {
                messageContent.put("content", nameOfRespondingUser + " accepted your deal");
                messageContent.put("title", "Tap to connect with " + nameOfRespondingUser);
            }
            else {
                messageContent.put("content", "Someone wants to make a deal, check it out!");
                messageContent.put("title", nameOfRespondingUser + " responded to your post!");
            }

            Message message = Message.builder()
                    .putData("type", "response")
                    .putAllData(messageContent)
                    .setToken(token)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Notification sent to " + id + ". Response: " + response);
            return new SuccessResult();
        } catch (FirebaseMessagingException e) {
            log.error("Error sending notif", e);
            return new ResultObject("internal", "Error sending notification, try again");
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
