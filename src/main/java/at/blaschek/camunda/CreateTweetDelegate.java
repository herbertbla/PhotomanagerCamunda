package at.blaschek.camunda;

import at.blaschek.camunda.components.TwitterService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author blascheh
 */
@Component
public class CreateTweetDelegate  implements JavaDelegate {

    final private TwitterService tweetDelegationBean;

    private final Logger LOGGER = LoggerFactory.getLogger(CreateTweetDelegate.class.getName());

    public CreateTweetDelegate(TwitterService tweetDelegationBean) {
        this.tweetDelegationBean = tweetDelegationBean;
    }


    public void execute(DelegateExecution execution) throws Exception {
        //der Contentstring muss sich ändern, sonst gibt es einen Fehler von Twitter
        String content =  ((String)execution.getVariable("content")) + new Date();
        tweetDelegationBean.writeTweet(content);
    }
}
