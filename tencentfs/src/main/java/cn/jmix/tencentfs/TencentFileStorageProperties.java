package cn.jmix.tencentfs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jmix.tencentfs")
@ConstructorBinding
public class TencentFileStorageProperties {
    String secretId;
    String secretKey;
    String bucket;
    int chunkSize;
    String endpointUrl;
    String region;

    public TencentFileStorageProperties(
            String secretId,
            String secretKey,
            String bucket,
            @DefaultValue("8192") int chunkSize,
            @DefaultValue("") String endpointUrl,
            String region) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.bucket = bucket;
        this.chunkSize = chunkSize;
        this.endpointUrl = endpointUrl;
        this.region = region;
    }

    /**
     *  access key.
     */
    public String getSecretId() {
        return secretId;
    }

    /**
     * secret access key.
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     *  bucket name.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     *  chunk size (kB).
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Return  storage endpoint URL.
     */
    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getRegion() {
        return region;
    }
}
