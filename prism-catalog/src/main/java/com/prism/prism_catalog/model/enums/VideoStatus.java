package com.prism.prism_catalog.model.enums;

public enum VideoStatus {
    PENDING,      // metadata created, file not uploaded yet
    UPLOADING,    // upload in progress
    UPLOADED,     // file stored, waiting for processing
    PROCESSING,   // transcoder working
    READY,        // fully processed & streamable
    FAILED,       // processing error
    DELETED       // soft-deleted
}
