package com.camunda.training;

import at.blaschek.camunda.CreateTweetDelegate;
import at.blaschek.camunda.components.TwitterService;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.mockito.Mockito.when;


public class ProcessJUnitTest {

    @Mock
    TwitterService db;

    // public ProcessEngineRule rule = new ProcessEngineRule();
    @Rule
    @ClassRule
    public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

    @Before
    public void setup() throws Exception {
        // in Memory Process Engine
        init(rule.getProcessEngine());
        MockitoAnnotations.initMocks(this);
        when(db.writeTweet(Mockito.any())).thenReturn(1L);
        Mocks.register("createTweetDelegate", new CreateTweetDelegate(new TwitterService()));
    }

    @Test
    @Deployment(resources = {"tweetApproval.dmn", "diagram_2.bpmn"})
    public void testHappyPath() {
        // Create a HashMap to put in variables for the process instance
        Map<String, Object> variables = new HashMap<String, Object>();
        // Start process with Java API and variables
        variables.put("email", "jakob.freund@camunda.com");
        variables.put("content", new Date().toString());

        // Prozessinstanz mit Variablen Starten
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);
        //assertThat(processInstance).isWaitingAt("TweetPruefenID");

        // job steht nun beim Synchronisationspunkt von Tweet veröffentlichen und muss weiter gesetzt werden - Beginn
        List<Job> jobList = jobQuery()
                .processInstanceId(processInstance.getId())
                .list();
        org.assertj.core.api.Assertions.assertThat(jobList).hasSize(1);
        Job job = jobList.get(0);


        execute(job);
        // job steht nun beim Synchronisationspunkt und muss weiter gesetzt werden - Ende

        assertThat(processInstance).isEnded();
    }


    /**
     * Aufgabe 8 - Start von einem bestimmten Punkt aus
     */
    @Test
    @Deployment(resources = {"tweetApproval.dmn", "diagram_2.bpmn"})
    public void testTweetRejected() {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("content", new Date().toString() + " HB");
        variables.put("approved", false);
        ProcessInstance processInstance = runtimeService()
                .createProcessInstanceByKey("TwitterQAProcess")
                .setVariables(variables)
                .startAfterActivity("TweetPruefenID")
                .execute();

        // job steht nun beim Synchronisationspunkt und muss weiter gesetzt werden - Ende

        assertThat(processInstance)
                .isWaitingAt("SendNotificationTask")
                .externalTask()
                .hasTopicName("notification");
        complete(externalTask());

        assertThat(processInstance).isEnded().hasPassed("TweetAbglehntEvent");
    }

    /**
     * Aufgabe 11 - Superuser Message
     */
    @Test
    @Deployment(resources = {"tweetApproval.dmn", "diagram_2.bpmn"})
    public void testSuperuserMessage() {
        ProcessInstance processInstance = runtimeService()
                .createMessageCorrelation("superuserTweet")
                .setVariable("content", "My Exercise 11 Tweet - " + System.currentTimeMillis())
                .correlateWithResult()
                .getProcessInstance();
        assertThat(processInstance).isStarted();
        runtimeService()
                .createMessageCorrelation("superuserTweet")
                .processInstanceId(processInstance.getId())
                .correlateWithResult();

        // get the job
        List<Job> jobList = jobQuery()
                .processInstanceId(processInstance.getId())
                .list();

        // execute the job
        //assertThat(jobList).hasSize(1);
        Job job = jobList.get(0);
        execute(job);

        assertThat(processInstance).isEnded();
    }



    @Test
    @Deployment(resources = {"tweetApproval.dmn", "diagram_2.bpmn"})
    public void testTweetFromJakob() {
        Map<String, Object> variables = withVariables("email", "jakob.freund@camunda.com", "content", "this should be published");
        DmnDecisionTableResult decisionResult = decisionService().evaluateDecisionTableByKey("tweetApproval", variables);
        org.assertj.core.api.Assertions.assertThat(decisionResult.getFirstResult()).contains(org.assertj.core.api.Assertions.entry("approved", true));

    }

    @Test
    @Deployment(resources = {"tweetApproval.dmn", "diagram_2.bpmn"})
    public void testTweetFromMe() {
        Map<String, Object> variables = withVariables("email", "herbert.xx@camunda.com", "content", "this should be published");
        DmnDecisionTableResult decisionResult = decisionService().evaluateDecisionTableByKey("tweetApproval", variables);
        org.assertj.core.api.Assertions.assertThat(decisionResult.getFirstResult()).contains(org.assertj.core.api.Assertions.entry("approved", false));

    }

    @Test
    @Deployment(resources = {"tweetApproval.dmn", "diagram_2.bpmn"})
    public void testTweetFromCamundaRocks() {
        Map<String, Object> variables = withVariables("email", "herbert.xx@camunda.com", "content", "camunda rocks");
        DmnDecisionTableResult decisionResult = decisionService().evaluateDecisionTableByKey("tweetApproval", variables);
        org.assertj.core.api.Assertions.assertThat(decisionResult.getFirstResult()).contains(org.assertj.core.api.Assertions.entry("approved", true));

    }

    @Test
    @Deployment(resources = {"tweetApproval.dmn", "diagram_2.bpmn"})
    public void testTweetFromCamundaRocksCannotTweet() {
        Map<String, Object> variables = withVariables("email", "cannot.tweet@camunda.com", "content", "camunda rocks");
        DmnDecisionTableResult decisionResult = decisionService().evaluateDecisionTableByKey("tweetApproval", variables);
        org.assertj.core.api.Assertions.assertThat(decisionResult.getFirstResult()).contains(org.assertj.core.api.Assertions.entry("approved", false));

    }
}
