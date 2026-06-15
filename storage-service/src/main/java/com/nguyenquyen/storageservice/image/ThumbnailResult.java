package com.nguyenquyen.storageservice.image;

public record ThumbnailResult(
        byte[] data,
        int width,
        int height
) {}