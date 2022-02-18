package cn.jmix.tencentfs;

import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

@ManagedResource(description = "Manages COS file storage client", objectName = "jmix.tencentfs:type=TencentFileStorage")
@Component("tencentfs_TencentFileStorageManagementFacade")
public class TencentStorageManagementFacade {
    @Autowired
    protected FileStorageLocator fileStorageLocator;

    @ManagedOperation(description = "Refresh COS file storage client")
    public String refreshCOSClient() {
        FileStorage fileStorage = fileStorageLocator.getDefault();
        if (fileStorage instanceof TencentFileStorage) {
            ((TencentFileStorage) fileStorage).refreshOssClient();
            return "Refreshed successfully";
        }
        return "Not an COS file storage - refresh attempt ignored";
    }

    @ManagedOperation(description = "Refresh COS client by storage name")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "storageName", description = "Storage name"),
            @ManagedOperationParameter(name = "accessKey", description = "COS access key"),
            @ManagedOperationParameter(name = "secretAccessKey", description = "COS secret access key")})
    public String refreshCOSClient(String storageName, String accessKey, String secretAccessKey) {
        FileStorage fileStorage = fileStorageLocator.getByName(storageName);
        if (fileStorage instanceof TencentFileStorage) {
            TencentFileStorage tencentFileStorage = (TencentFileStorage) fileStorage;
            tencentFileStorage.setSecretId(accessKey);
            tencentFileStorage.setSecretKey(secretAccessKey);
            tencentFileStorage.refreshOssClient();
            return "Refreshed successfully";
        }
        return "Not an COS file storage - refresh attempt ignored";
    }

    @ManagedOperation(description = "Refresh COS file storage client by storage name")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "storageName", description = "Storage name"),
            @ManagedOperationParameter(name = "accessKey", description = "COS access key"),
            @ManagedOperationParameter(name = "secretAccessKey", description = "COS secret access key"),
            @ManagedOperationParameter(name = "bucket", description = "COS bucket name"),
            @ManagedOperationParameter(name = "chunkSize", description = "COS chunk size (kB)"),
            @ManagedOperationParameter(name = "endpointUrl", description = "Optional custom COS storage endpoint URL")})
    public String refreshCOSClient(String storageName, String accessKey, String secretAccessKey,
                                  String region, String bucket, int chunkSize, @Nullable String endpointUrl) {
        FileStorage fileStorage = fileStorageLocator.getByName(storageName);
        if (fileStorage instanceof TencentFileStorage) {
            TencentFileStorage tencentFileStorage = (TencentFileStorage) fileStorage;
            tencentFileStorage.setSecretId(accessKey);
            tencentFileStorage.setSecretKey(secretAccessKey);
            tencentFileStorage.setRegion(region);
            tencentFileStorage.setBucket(bucket);
            tencentFileStorage.setChunkSize(chunkSize);
            tencentFileStorage.setEndpointUrl(endpointUrl);
            tencentFileStorage.refreshOssClient();
            return "Refreshed successfully";
        }
        return "Not an COS file storage - refresh attempt ignored";
    }
}
