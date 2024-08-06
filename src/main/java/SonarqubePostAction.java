import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class SonarqubePostAction {

    private static final Logger logger = Logger.getLogger(SonarqubePostAction.class.getName());

    private static final Gson gson = new Gson();
    private static final String SUCCESS = "SUCCESS";
    private static final String FILE_PATH = "./target/sonar/report-task.txt";
    private static final String SLACK_MENTION = "<!subteam^S04QGJY6290>";
    private static String slackWebhookUrl = null;
    private static String token = null;
    private static String projectKey = null;
    private static String serverUrl = null;
    private static String executedAt = null;
    private static String pullRequestUrl = null;
    private static String pullRequestTitle = null;

    public static void main(String[] args) {

        logger.info("Starting Sonarqube Post Action");
        token = System.getProperty("sonarToken") + ":";
        logger.info(() -> "sonarToken = " + token);
        slackWebhookUrl = System.getProperty("slackWebhook");
        logger.info(() -> "slackWebhook = " + slackWebhookUrl);
        pullRequestUrl = System.getProperty("pullRequestUrl");
        logger.info(() -> "pullRequestUrl = " + pullRequestUrl);
        pullRequestTitle = System.getProperty("pullRequestTitle");
        logger.info(() -> "pullRequestTitle = " + pullRequestTitle);

        try (FileInputStream fis = new FileInputStream(FILE_PATH)) {
            var properties = new Properties();
            properties.load(fis);
            logger.info(() -> "properties = " + properties);

            projectKey = properties.getProperty("projectKey");
            serverUrl = properties.getProperty("serverUrl");
            var ceTaskUrl = properties.getProperty("ceTaskUrl");
            if (ceTaskUrl != null) {
                logger.info(() -> "ceTaskUrl: " + ceTaskUrl);
                if (isTaskSuccess(ceTaskUrl)) {
                    getAllIssues();
                }
            } else {
                logger.info("ceTaskUrl not found in the file.");
            }
        } catch (IOException | InterruptedException e) {
            logger.severe(e.getMessage());
        }
    }

    private static boolean isTaskSuccess(String url) throws InterruptedException {
        logger.info(() -> "isTaskSuccess = " + url);
        var response = callApi(url);
        var task = (Map) response.get("task");
        var isSuccess = SUCCESS.equals(task.get("status"));
        if (isSuccess) {
            executedAt = (String) task.get("executedAt");
        } else {
            Thread.sleep(10000);
            isSuccess = isTaskSuccess(url);
        }
        return isSuccess;
    }

    private static void getAllIssues() {
        var executedDate = parseToLocalDateTime(executedAt);
        var createdAfter = executedDate.minusMinutes(15).format(DateTimeFormatter.ISO_DATE_TIME) + "%2B0000";
        var createdBefore = executedDate.plusMinutes(2).format(DateTimeFormatter.ISO_DATE_TIME) + "%2B0000";
        var url = serverUrl + "/api/issues/search?componentKeys=" + projectKey + "&createdAfter=" + createdAfter + "&createdBefore=" + createdBefore + "&issueStatuses=OPEN";
        var response = callApi(url);
        sendSlackMessage(response);
    }

    private static LocalDateTime parseToLocalDateTime(String dateTimeString) {
        logger.info(() -> "parseToLocalDateTime = " + dateTimeString);
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        var offsetDateTime = OffsetDateTime.parse(dateTimeString, formatter);
        return offsetDateTime.toLocalDateTime();
    }

    private static Map<String, Object> callApi(String url) {
        logger.info(() -> "callApi: " + url);
        try {
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(token.getBytes()))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                logger.info(() -> "Response: " + response.body());
                return gson.fromJson(response.body(), Map.class);
            } else {
                logger.severe(() -> "Error: " + response.statusCode());
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }

        return Map.of();
    }

    private static void sendSlackMessage(Map<String, Object> response) {
        logger.info(() -> "response: " + response);
        var issues = (List<Map>) response.get("issues");
        StringBuilder text = new StringBuilder(SLACK_MENTION);
        text.append(" ").append(issues.size()).append(" open issues found after scanning ")
                .append("<").append(pullRequestUrl).append("|PR> with title `").append(pullRequestTitle).append("`");
        if (!issues.isEmpty()) {
            text.append("\n").append(">*New Issues*\n");
            for (var issue : issues) {
                var sqUrl = serverUrl + "/project/issues?open=" + issue.get("key") + "&id=" + projectKey;
                text.append("> - ").append(issue.get("message")).append(" <").append(sqUrl).append("|open> \n");
            }
        }

        var json = new HashMap<>();
        json.put("text", text.toString());
        json.put("channel", "#notif-sonarqube-dev");
        json.put("username", "Sonarqube DEV");
        json.put("icon_url", "https://avatars.slack-edge.com/2023-11-22/6261573885616_c6ebfdcc94d8fc330f9f_192.png");

        sendMessage(gson.toJson(json, Map.class));
    }

    private static void sendMessage(String message) {
        logger.info(() -> "sendMessage: " + message);
        try {
            var client = HttpClient.newHttpClient();
            var bodyPublisher = HttpRequest.BodyPublishers.ofString(message);
            var request = HttpRequest.newBuilder()
                    .uri(new URI(slackWebhookUrl))
                    .POST(bodyPublisher)
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info(() -> "Response: " + response.body());
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }

    }

}
