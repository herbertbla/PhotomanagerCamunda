package at.blaschek.camunda.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

/**
 * @author blascheh
 */
@Component
public class TwitterService {
    private final Logger LOGGER = LoggerFactory.getLogger(TwitterService.class.getName());

    public Long writeTweet(String content) throws Exception {
        //der Contentstring muss sich ändern, sonst gibt es einen Fehler von Twitter
        LOGGER.info("Publishing tweet: " + content);
        AccessToken accessToken = new AccessToken("220324559-jet1dkzhSOeDWdaclI48z5txJRFLCnLOK45qStvo", "B28Ze8VDucBdiE38aVQqTxOyPc7eHunxBVv7XgGim4say");
        Twitter twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer("lRhS80iIXXQtm6LM03awjvrvk", "gabtxwW8lnSL9yQUNdzAfgBOgIMSRqh7MegQs79GlKVWF36qLS");
        twitter.setOAuthAccessToken(accessToken);
        return twitter.updateStatus(content).getId();
    }
}
