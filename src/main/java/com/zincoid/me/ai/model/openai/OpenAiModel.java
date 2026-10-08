package com.zincoid.me.ai.model.openai;

import com.zincoid.me.ai.model.Model;
import com.zincoid.me.ai.model.ModelReq;
import com.zincoid.me.ai.model.ModelRes;
import com.zincoid.me.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OpenAiModel implements Model {

    private final OpenAiProperties openAiProperties;
    private final Map<String, RestClient> restClients = new ConcurrentHashMap<>();

    public OpenAiModel(OpenAiProperties openAiProperties) {
        this.openAiProperties = openAiProperties;
        if (openAiProperties.getProviders() != null)
            openAiProperties.getProviders().forEach(p ->
                    log.info("AI provider registered: name={}, model={}", p.getName(), p.getModel()));
    }

    @Override
    public ModelRes invoke(ModelReq req) {
        OpenAiProperties.Provider provider = openAiProperties.current();
        OpenAiReq apiReq = OpenAiReq.from(req, provider.getModel());
        OpenAiRes apiRes;
        try {
            apiRes = getClient(provider).post()
                    .uri(resolveUrl(provider))
                    .body(apiReq)
                    .retrieve()
                    .body(OpenAiRes.class);
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            boolean insufficient = e.getStatusCode().is4xxClientError() && (
                    body.contains("Insufficient Balance")
                            || body.contains("InsufficientBalance")
                            || body.contains("insufficient_balance")
                            || body.contains("insufficient_quota")
                            || body.contains("quota")
                            || body.contains("余额"));
            if (insufficient) {
                log.warn("AI API insufficient balance ({}): {}", provider.getName(), body);
                throw new BusinessException(502, "Model API has insufficient balance");
            }
            throw e;
        }
        if (apiRes == null)
            throw new BusinessException(502, "Empty response from AI model API");
        return apiRes.toModelRes();
    }

    private RestClient getClient(OpenAiProperties.Provider provider) {
        String key = provider.getName() != null ? provider.getName() : provider.getApiUrl();
        return restClients.computeIfAbsent(key, name -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10_000);
            factory.setReadTimeout(180_000);
            return RestClient.builder()
                    .requestFactory(factory)
                    .defaultHeader("Authorization", "Bearer " + provider.getApiKey())
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .build();
        });
    }

    private String resolveUrl(OpenAiProperties.Provider provider) {
        String url = provider.getApiUrl();
        if (url.endsWith("/chat/completions")) return url;
        return url.endsWith("/") ? url + "chat/completions" : url + "/chat/completions";
    }
}
