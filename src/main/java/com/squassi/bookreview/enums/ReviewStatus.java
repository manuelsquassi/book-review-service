package com.squassi.bookreview.enums;

/**
 * Represents the processing status of a review.
 * Reviews go through different states during their lifecycle.
 */
public enum ReviewStatus {
    
    /**
     * Review has been created and is awaiting processing
     */
    PROCESSING("Processing"),
    
    /**
     * Review has been successfully processed and enriched with book metadata
     */
    READY("Ready"),
    
    /**
     * An error occurred during review processing
     */
    ERROR("Error");
    
    private final String displayName;
    
    ReviewStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return this.name();
    }
}
