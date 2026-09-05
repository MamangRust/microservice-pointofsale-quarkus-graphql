package com.sanedge.gateway.service;

import org.jboss.resteasy.reactive.multipart.FileUpload;

public interface FileService {
    String createFileImage(FileUpload file, String filepath);
    String createFileImageBase64(String base64Data, String filepath);
    void deleteFileImage(String filepath);
}
