package xyz.crearts.note.keeper.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * DingTalk webhook client for sending messages.
 * API: https://open.dingtalk.com/document/robots/custom-robot-access
 */
@Component
public class DingTalkClient {

    private static final Logger log = LoggerFactory.getLogger(DingTalkClient.class);

    private final RestClient restClient;

    public DingTalkClient() {
        this.restClient = RestClient.create();
    }

    /**
     * Send text message to DingTalk webhook.
     * @param webhookUrl DingTalk webhook URL
     * @param secret Optional secret for signature
     * @param text message text
     * @return true if sent successfully
     */
    public boolean sendMessage(String webhookUrl, String secret, String text) {
        if (webhookUrl == null) {
            log.warn("DingTalk integration not configured (webhookUrl={})", webhookUrl != null);
            return false;
        }

        try {
            String url = buildSignedUrl(webhookUrl, secret);
            
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "text");
            
            Map<String, String> textContent = new HashMap<>();
            textContent.put("content", text);
            body.put("text", textContent);

            DingTalkResponse response = restClient.post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .body(DingTalkResponse.class);

            if (response != null && response.getErrcode() == 0) {
                log.info("DingTalk message sent successfully");
                return true;
            } else {
                log.error("DingTalk API returned error: {}", response != null ? response.getErrmsg() : "null");
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send DingTalk message", e);
            return false;
        }
    }

    /**
     * Build signed URL for DingTalk webhook.
     * If secret is configured, adds timestamp and signature parameters.
     */
    private String buildSignedUrl(String webhookUrl, String secret) throws Exception {
        if (secret == null || secret.isEmpty()) {
            return webhookUrl;
        }

        long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + secret;
        
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);

        return webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;
    }

    private static class DingTalkResponse {
        private Integer errcode;
        private String errmsg;

        public Integer getErrcode() {
            return errcode;
        }

        public void setErrcode(Integer errcode) {
            this.errcode = errcode;
        }

        public String getErrmsg() {
            return errmsg;
        }

        public void setErrmsg(String errmsg) {
            this.errmsg = errmsg;
        }
    }
}
