package org.bonitasoft.store.InputArtifact;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Describe a source to access the content of an artifact
 *
 */
public abstract class BonitaStoreInput {
    
    public abstract long lastModified();
    
    public abstract String getContentText() throws Exception;
    
    public abstract InputStream getContentInputStream() throws FileNotFoundException;
    
    public abstract Object getSignature();

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* InputFile from File */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    
    
}