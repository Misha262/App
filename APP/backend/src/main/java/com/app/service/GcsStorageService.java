package com.app.service;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.UUID;

public class GcsStorageService {

    private final Storage storage;
    private final String bucket;

    public GcsStorageService(String bucket) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucket = bucket;
    }

    public String uploadFile(int groupId, MultipartFile file) throws Exception {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String objectName = "group-" + groupId + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, objectName))
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());
        return objectName;
    }

    public InputStream download(String objectName) {
        Blob blob = storage.get(BlobId.of(bucket, objectName));
        if (blob == null) return null;
        ReadChannel reader = blob.reader();
        return Channels.newInputStream(reader);
    }

    public String getContentType(String objectName) {
        Blob blob = storage.get(BlobId.of(bucket, objectName));
        return blob != null ? blob.getContentType() : null;
    }

    public long getSize(String objectName) {
        Blob blob = storage.get(BlobId.of(bucket, objectName));
        return blob != null ? blob.getSize() : 0L;
    }

    public void delete(String objectName) {
        storage.delete(BlobId.of(bucket, objectName));
    }
}
