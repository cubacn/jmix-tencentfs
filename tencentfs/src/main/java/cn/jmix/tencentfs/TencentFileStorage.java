package cn.jmix.tencentfs;


import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import io.jmix.core.*;
import io.jmix.core.annotation.Internal;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Internal
@Component("tencentfs_FileStorage")
public class TencentFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(TencentFileStorage.class);
    public static final String DEFAULT_STORAGE_NAME = "bos";

    protected String storageName;

    @Autowired
    protected TencentFileStorageProperties properties;

    boolean useConfigurationProperties = true;

    protected String secretId;
    protected String secretKey;
    protected String region;
    protected String bucket;
    protected int chunkSize;
    protected String endpointUrl;

    @Autowired
    protected TimeSource timeSource;

    protected AtomicReference<COSClient> clientReference = new AtomicReference<>();

    public TencentFileStorage() {
        this(DEFAULT_STORAGE_NAME);
    }

    public TencentFileStorage(String storageName) {
        this.storageName = storageName;
    }

    /**
     * Optional constructor that allows you to override {@link TencentFileStorageProperties}.
     */
    public TencentFileStorage(
            String storageName,
            String secretId,
            String secretKey,
            String bucket,
            String region,
            int chunkSize,
            @Nullable String endpointUrl) {
        this.useConfigurationProperties = false;
        this.storageName = storageName;
        this.secretId = secretId;
        this.region=region;
        this.secretKey = secretKey;
        this.bucket = bucket;
        this.chunkSize = chunkSize;
    }

    @EventListener
    public void initOssClient(ApplicationStartedEvent event) {
        refreshOssClient();
    }

    protected void refreshProperties() {
        if (useConfigurationProperties) {
            this.secretId = properties.getSecretId();
            this.secretKey = properties.getSecretKey();
            this.bucket = properties.getBucket();
            this.chunkSize = properties.getChunkSize();
            this.region = properties.getRegion();
        }
    }


    public void refreshOssClient() {
        refreshProperties();
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region region = new Region(this.region);
        ClientConfig clientConfig= new ClientConfig(region);
        clientConfig.setHttpProtocol(HttpProtocol.https);
        COSClient cosClient = new COSClient(cred, clientConfig);
        clientReference.set( cosClient);
    }

    @Override
    public String getStorageName() {
        return storageName;
    }

    protected String createFileKey(String fileName) {
        return createDateDir() + "/" + createUuidFilename(fileName);
    }

    protected String createDateDir() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timeSource.currentTimestamp());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return String.format("%d/%s/%s", year,
                StringUtils.leftPad(String.valueOf(month), 2, '0'),
                StringUtils.leftPad(String.valueOf(day), 2, '0'));
    }

    protected String createUuidFilename(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        if (StringUtils.isNotEmpty(extension)) {
            return UuidProvider.createUuid().toString() + "." + extension;
        } else {
            return UuidProvider.createUuid().toString();
        }
    }

    private String claimUploadId(String objectName) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, objectName);
        InitiateMultipartUploadResult result = clientReference.get().initiateMultipartUpload(request);
        return result.getUploadId();
    }

    private void completeMultipartUpload(List<PartETag> partETags, String objectName, String uploadId) {
        // Make part numbers in ascending order
        Collections.sort(partETags, new Comparator<PartETag>() {
            @Override
            public int compare(PartETag p1, PartETag p2) {
                return p1.getPartNumber() - p2.getPartNumber();
            }
        });
        log.info("Completing to upload multiparts\n");
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucket, objectName, uploadId, partETags);
        clientReference.get().completeMultipartUpload(completeMultipartUploadRequest);
    }

    private void listAllParts(String objectName, String uploadId) {
        log.debug("Listing all parts......");
        ListPartsRequest listPartsRequest = new ListPartsRequest(bucket, objectName, uploadId);
        PartListing partListing = clientReference.get().listParts(listPartsRequest);
        int partCount = partListing.getParts().size();
        for (int i = 0; i < partCount; i++) {
            PartSummary partSummary = partListing.getParts().get(i);
            log.debug("\tPart#" + partSummary.getPartNumber() + ", ETag=" + partSummary.getETag());
        }
        log.debug("\n");
    }

    @Override
    public FileRef saveStream(String fileName, InputStream inputStream) {
        String fileKey = createFileKey(fileName);
        try {
            byte[] data = IOUtils.toByteArray(inputStream);
            COSClient client = clientReference.get();
            int chunkSizeBytes = this.chunkSize * 1024;
            List<PartETag> partETags = new ArrayList<>();

            String uploadId = claimUploadId(fileKey);
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            int partCount = 0;
            for (int i = 0; i * chunkSizeBytes < data.length; i++) {
                partCount++;
                int partNumber = i + 1;
                int endChunkPosition = Math.min(partNumber * chunkSizeBytes, data.length);
                byte[] chunkBytes = getChunkBytes(data, i * chunkSizeBytes, endChunkPosition);

                PartUploader partUploader =
                        new PartUploader(client, partETags, chunkBytes, fileKey, bucket, chunkBytes.length, partNumber, uploadId);
                executorService.execute(partUploader);
            }
            executorService.shutdown();
            while (!executorService.isTerminated()) {
                try {
                    executorService.awaitTermination(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, "Uploading file to bos failed", e);
                } finally {
                    executorService.shutdownNow();
                }
            }
            if (partETags.size() != partCount) {
                throw new IllegalStateException("Upload multiparts fail due to some parts are not finished yet");
            } else {
                log.info("Succeed to complete multiparts into an object named " + fileKey + "\n");
            }

            listAllParts(fileKey, uploadId);
            completeMultipartUpload(partETags, fileKey, uploadId);
            return new FileRef(getStorageName(), fileKey, fileName);
        } catch (IOException e) {
            String message = String.format("Could not save file %s.", fileName);
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    protected byte[] getChunkBytes(byte[] data, int start, int end) {
        byte[] chunkBytes = new byte[end - start];
        System.arraycopy(data, start, chunkBytes, 0, end - start);
        return chunkBytes;
    }

    @Override
    public InputStream openStream(FileRef reference) {
        try {
            COSClient client = clientReference.get();
            COSObject object = client.getObject(new GetObjectRequest(bucket, reference.getPath()));
            return object.getObjectContent();
        } catch (Exception e) {
            String message = String.format("Could not load file %s.", reference.getFileName());
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    @Override
    public void removeFile(FileRef reference) {
        try {
            COSClient client = clientReference.get();
            DeleteObjectRequest request = new DeleteObjectRequest(bucket,reference.getPath());
            client.deleteObject(request);
        } catch (Exception e) {
            String message = String.format("Could not delete file %s.", reference.getFileName());
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    @Override
    public boolean fileExists(FileRef reference) {
        COSClient client = clientReference.get();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.setBucketName(bucket);
        listObjectsRequest.setPrefix(reference.getPath());
        listObjectsRequest.setMaxKeys(1);
        ObjectListing objectListing = client.listObjects(listObjectsRequest);
        List<COSObjectSummary> summaries = objectListing.getObjectSummaries();
        return summaries.size() > 0;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setEndpointUrl(@Nullable String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
}
