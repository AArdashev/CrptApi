import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final AtomicInteger requestCount;
    private long lastRequestTimeMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requestCount = new AtomicInteger(0);
        this.lastRequestTimeMillis = System.currentTimeMillis();
    }

    public synchronized void createDocument(Object document, String signature) throws IOException {
        long currentTimeMillis = System.currentTimeMillis();
        long timePassed = currentTimeMillis - lastRequestTimeMillis;
        long timeLimitMillis = timeUnit.toMillis(1);

        if (timePassed >= timeLimitMillis) {
            requestCount.set(0);
            lastRequestTimeMillis = currentTimeMillis;
        }

        if (requestCount.get() >= requestLimit) {
            try {
                wait(timeLimitMillis - timePassed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Отправка запроса на создание документа
        sendRequest(document, signature);

        requestCount.incrementAndGet();
    }

    private void sendRequest(Object document, String signature) throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonDocument = objectMapper.createObjectNode();
        jsonDocument.put("document", objectMapper.writeValueAsString(document));
        jsonDocument.put("signature", signature);

        StringEntity entity = new StringEntity(jsonDocument.toString());
        httpPost.setEntity(entity);
        httpPost.setHeader("Content-Type", "application/json");

        HttpResponse response = client.execute(httpPost);

        // Обработка ответа сервера, если необходимо
    }
}
