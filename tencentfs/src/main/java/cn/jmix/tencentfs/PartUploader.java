package cn.jmix.tencentfs;


import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;
import io.jmix.core.FileStorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class PartUploader implements Runnable {

    private final String bucketName;
    private final String objectName;
    private final COSClient client;
    private final List<PartETag> partETags;
    private final byte[] data;
    
    private final long partSize;
    private final int partNumber;
    private final String uploadId;


    public PartUploader(COSClient  client, List<PartETag> partETags, byte[] chunkedData,
                        String objectName,
                        String bucketName,
                        long partSize,
                        int partNumber, String uploadId) {
        this.data=chunkedData;
        this.client=client;
        this.partETags=partETags;
        this.partSize = partSize;
        this.bucketName=bucketName;
        this.partNumber = partNumber;
        this.uploadId = uploadId;
        this.objectName=objectName;
    }

    @Override
    public void run() {
        InputStream instream = null;
        try {

            instream = new ByteArrayInputStream(this.data);
//            instream.skip(this.startPos);
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(objectName);
            uploadPartRequest.setUploadId(this.uploadId);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartSize(this.partSize);
            uploadPartRequest.setPartNumber(this.partNumber);

            UploadPartResult uploadPartResult = client.uploadPart(uploadPartRequest);
            synchronized (partETags) {
                partETags.add(uploadPartResult.getPartETag());
            }
        } catch (Exception e) {
          throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION,"uploading a part of data failed",e);
        } finally {
            if (instream != null) {
                try {
                    instream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }





}
