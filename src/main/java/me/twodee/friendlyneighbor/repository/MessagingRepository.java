package me.twodee.friendlyneighbor.repository;

import me.twodee.friendlyneighbor.entity.MessageRecipient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.inject.Inject;
import java.util.List;

public class MessagingRepository {

    private final MongoTemplate template;

    @Inject
    MessagingRepository(MongoTemplate template) {
        this.template = template;
    }

    public MessageRecipient save(MessageRecipient messageRecipient) {
        return template.save(messageRecipient);
    }

    public List<MessageRecipient> findTokensByIds(List<String> ids) {
        return template.find(Query.query(Criteria.where("userId").in(ids)), MessageRecipient.class);
    }

    public MessageRecipient findById(String id) {
        return template.findById(id, MessageRecipient.class);
    }
}
