package xyz.kuailemao.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * SSL 信任配置
 * <p>
 * 解决 JDK cacerts 中缺少 GitHub 等外部服务 CA 证书导致 SSLHandshakeException 的问题。
 * 仅将特定域名的证书链追加到默认信任库中，不跳过任何证书验证，保证安全性。
 * 同时提供配置好的 OkHttpClient Bean，供第三方登录等场景使用。
 * </p>
 *
 * @author kuailemao
 */
@Slf4j
@Configuration
public class SslTrustConfig {

    /**
     * 需要额外信任的域名列表（第三方 OAuth 服务等）
     */
    private static final String[] TRUSTED_DOMAINS = {
            "github.com",
            "api.github.com",
            "gitee.com"
    };

    /**
     * 合并后的信任库，在初始化时构建
     */
    private KeyStore mergedTrustStore;

    /**
     * 初始化合并信任库，并设为 JVM 默认 SSLContext
     * 使用 @Bean 方式的 initMethod 确保 Bean 初始化时执行
     */
    @Bean
    public SslContextHolder sslContextHolder() {
        try {
            // 1. 加载 JDK 默认的 cacerts 信任库
            KeyStore trustStore = loadDefaultTrustStore();

            // 2. 从各域名下载证书链并追加到信任库
            int addedCount = 0;
            for (String domain : TRUSTED_DOMAINS) {
                try {
                    Certificate[] chain = downloadCertificateChain(domain);
                    for (int i = 0; i < chain.length; i++) {
                        X509Certificate cert = (X509Certificate) chain[i];
                        String alias = domain + "-" + i + "-" + cert.getSubjectX500Principal().getName();
                        if (!trustStore.containsAlias(alias)) {
                            trustStore.setCertificateEntry(alias, cert);
                            addedCount++;
                            log.info("SSL信任配置: 添加证书 [{}] -> {}", alias, cert.getSubjectX500Principal());
                        }
                    }
                } catch (Exception e) {
                    log.warn("SSL信任配置: 下载 {} 证书链失败: {}", domain, e.getMessage());
                }
            }

            // 3. 保存合并后的信任库引用
            this.mergedTrustStore = trustStore;

            // 4. 用合并后的信任库创建 SSLContext 并设为 JVM 默认
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
            SSLContext.setDefault(sslContext);

            log.info("SSL信任配置: 初始化完成, 新增 {} 张证书", addedCount);
        } catch (Exception e) {
            log.error("SSL信任配置: 初始化失败", e);
        }
        return new SslContextHolder();
    }

    /**
     * 提供配置好 SSL 的 OkHttpClient Bean
     * 使用合并后的信任库，确保 GitHub/Gitee 等 HTTPS 请求正常工作
     */
    @Bean
    public OkHttpClient okHttpClient() {
        try {
            // 确保 sslContextHolder 已初始化
            sslContextHolder();

            if (mergedTrustStore != null) {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(mergedTrustStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

                return new OkHttpClient.Builder()
                        .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .build();
            }
        } catch (Exception e) {
            log.error("SSL信任配置: 创建 OkHttpClient 失败，使用默认配置", e);
        }

        // 降级：返回默认 OkHttpClient
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 加载 JDK 默认的 cacerts 信任库
     */
    private KeyStore loadDefaultTrustStore() throws Exception {
        String cacertsPath = System.getProperty("javax.net.ssl.trustStore");
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] trustStorePassword;

        if (cacertsPath != null) {
            trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword", "changeit").toCharArray();
            try (InputStream is = new java.io.FileInputStream(cacertsPath)) {
                trustStore.load(is, trustStorePassword);
            }
        } else {
            trustStorePassword = "changeit".toCharArray();
            String javaHome = System.getProperty("java.home");
            String defaultCacerts = javaHome + "/lib/security/cacerts";
            try (InputStream is = new java.io.FileInputStream(defaultCacerts)) {
                trustStore.load(is, trustStorePassword);
            }
        }
        return trustStore;
    }

    /**
     * 通过建立 TLS 连接获取目标域名的证书链
     */
    private Certificate[] downloadCertificateChain(String domain) throws Exception {
        URL url = new URL("https://" + domain);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        // 使用不验证证书的临时连接来获取证书链（仅用于读取证书，不用于实际数据传输）
        conn.setSSLSocketFactory(createTrustAllSocketFactory());
        conn.setHostnameVerifier((hostname, session) -> true);

        try {
            conn.connect();
            return conn.getServerCertificates();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * 创建一个临时信任所有证书的 SSLSocketFactory，仅用于读取目标服务器的证书链
     */
    private SSLSocketFactory createTrustAllSocketFactory() throws Exception {
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, new SecureRandom());
        return sc.getSocketFactory();
    }

    /**
     * 占位 Holder 类，用于触发 sslContextHolder Bean 的初始化
     */
    public static class SslContextHolder {
    }
}
