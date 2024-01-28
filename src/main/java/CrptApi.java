import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final Object lock = new Object();
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private long lastResetTimeMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.lastResetTimeMillis = System.currentTimeMillis();
    }

    public void createDocument(Document document) {
        synchronized (lock) {
            resetIfNecessary();
            if (requestCount.get() >= requestLimit) {
                System.out.println("Request limit exceeded. Request blocked.");
                return;
            }

            String jsonDocument = new Gson().toJson(document);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
                httpPost.setEntity(new StringEntity(jsonDocument));
                httpPost.setHeader("Content-Type", "application/json");

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    HttpEntity responseEntity = response.getEntity();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(responseEntity.getContent()));
                    String line;
                    StringBuilder responseContent = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        responseContent.append(line);
                    }
                    System.out.println("Response: " + responseContent.toString());
                }

                requestCount.incrementAndGet();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetIfNecessary() {
        long currentTimeMillis = System.currentTimeMillis();
        long timeElapsed = currentTimeMillis - lastResetTimeMillis;
        long timeLimitMillis = timeUnit.toMillis(1); // 1 unit of the timeUnit

        if (timeElapsed >= timeLimitMillis) {
            requestCount.set(0); // Reset request count
            lastResetTimeMillis = currentTimeMillis; // Update last reset time
        }
    }

    static class Document {
        @SerializedName("description")
        Description description;
        @SerializedName("doc_id")
        String docId;
        @SerializedName("doc_status")
        String docStatus;
        @SerializedName("doc_type")
        String docType;
        @SerializedName("importRequest")
        boolean importRequest;
        @SerializedName("owner_inn")
        String ownerInn;
        @SerializedName("participant_inn")
        String participantInn;
        @SerializedName("producer_inn")
        String producerInn;
        @SerializedName("production_date")
        String productionDate;
        @SerializedName("production_type")
        String productionType;
        @SerializedName("products")
        Product[] products;
        @SerializedName("reg_date")
        String regDate;
        @SerializedName("reg_number")
        String regNumber;
    }

    static class Description {
        @SerializedName("participantInn")
        String participantInn;
    }

    static class Product {
        @SerializedName("certificate_document")
        String certificateDocument;
        @SerializedName("certificate_document_date")
        String certificateDocumentDate;
        @SerializedName("certificate_document_number")
        String certificateDocumentNumber;
        @SerializedName("owner_inn")
        String ownerInn;
        @SerializedName("producer_inn")
        String producerInn;
        @SerializedName("production_date")
        String productionDate;
        @SerializedName("tnved_code")
        String tnvedCode;
        @SerializedName("uit_code")
        String uitCode;
        @SerializedName("uitu_code")
        String uituCode;
    }
}
