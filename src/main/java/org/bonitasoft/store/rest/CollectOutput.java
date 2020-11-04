package org.bonitasoft.store.rest;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStoreDirectory;
import org.json.simple.JSONValue;
import org.omg.CORBA_2_3.portable.OutputStream;

public class CollectOutput {

    private final static BEvent eventFileCopyError = new BEvent(CollectOutput.class.getName(), 1, Level.APPLICATIONERROR, 
            "File copy error", "A file copy failed", "The output of the REST API is not collected", "Check Exception");
    private final static BEvent eventOutputStreamCopyError = new BEvent(CollectOutput.class.getName(), 2, Level.APPLICATIONERROR, 
            "OutputStream copy error", "Copy to the outputStream failed", "The output of the REST API is not collected", "Check Exception");

    

    /**
     * choose the policy to collect the output
     * - ignoreResponse : to nothing
     * - transformToString : get the result in the string "body"
     * - transformToByteArray : get the result in the bytearray "baos"
     * - transformByMyself : call the method collectEntity() : class can be overrited it.
     */
    public enum POLICYOUTPUT { IGNORE, STRING, JSON, BYTEARRAY, SPECIFIC, FILENAME, OUTSTREAM }
    private POLICYOUTPUT policyOutput = POLICYOUTPUT.STRING;
    private String body;

    private ByteArrayOutputStream baos;


    private StringBuffer traceCollect;

    private String fileName;
    
    private OutputStream outputStream;
    
    
    private List<BEvent> listEvents = new ArrayList<>();
    
    

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* getInstance */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public static CollectOutput getInstanceString() {
        CollectOutput collectOutput = new CollectOutput();
        collectOutput.policyOutput = POLICYOUTPUT.STRING;
        return collectOutput;
    }
    public static CollectOutput getInstanceJson() {
        CollectOutput collectOutput = new CollectOutput();
        collectOutput.policyOutput = POLICYOUTPUT.JSON;
        return collectOutput;
    }
    public static CollectOutput getInstanceFileName( String fileName ) {
        CollectOutput collectOutput = new CollectOutput();
        collectOutput.setFileName(fileName); 
        return collectOutput;
    }
    public void setPolicy(POLICYOUTPUT policy) {
        this.policyOutput = policy;
    }
    

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Setter */
    /*                                                                      */
    /* -------------------------------------------------------------------- */



    /**
     *  in case of policy FILENAME : give the file name to save the output
     * @param fileName
     */
    public void setFileName( String fileName ) {
        this.policyOutput=POLICYOUTPUT.FILENAME;
        this.fileName = fileName;
    }
    public void setOutputStream(OutputStream outputStream) {
        this.policyOutput=POLICYOUTPUT.OUTSTREAM;
        this.outputStream = outputStream;
    }
 

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Collect the output */
    /*                                                                      */
    /* -------------------------------------------------------------------- */



    public void collectHttpResponse(HttpResponse httpResponse) throws UnsupportedOperationException, IOException {
        traceCollect = new StringBuffer();
        listEvents.clear();
        final HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            traceCollect.append("No entity");
            return;
        }
        if (POLICYOUTPUT.IGNORE.equals( policyOutput )) {
            EntityUtils.consumeQuietly(entity);
        } else if (POLICYOUTPUT.STRING.equals( policyOutput ) || POLICYOUTPUT.JSON.equals( policyOutput )) {
            final InputStream inputStream = entity.getContent();

            final StringWriter stringWriter = new StringWriter();
            IOUtils.copy(inputStream, stringWriter,RESTCharsets.UTF_8.getValue());
            if (stringWriter.toString() != null) {
                body=stringWriter.toString();
                traceCollect.append(" Body[" + stringWriter.toString() + "]");
            }

        } else if (POLICYOUTPUT.BYTEARRAY.equals( policyOutput )) {
            baos = new ByteArrayOutputStream();
            entity.writeTo(baos);
            traceCollect.append(" Body:byte[" + baos.toByteArray().length + "]");
        } else if (POLICYOUTPUT.FILENAME.equals( policyOutput )) {
            traceCollect.append("Write to["+fileName+"]");
            try (FileOutputStream fileOutput = new FileOutputStream( fileName )) {
                IOUtils.copy(entity.getContent(), fileOutput);
            } catch(Exception e) {
                listEvents.add( new BEvent(eventFileCopyError, e, "File["+fileName+"]"));
            }
        } else if (POLICYOUTPUT.OUTSTREAM.equals( policyOutput )) {
            traceCollect.append("Write to outputStream");
            try {
                IOUtils.copy(entity.getContent(), outputStream);
            } catch(Exception e) {
                listEvents.add( new BEvent(eventOutputStreamCopyError, e, ""));
            }} else  {
            traceCollect.append(" ManageEntity");
            collectEntity(entity);
        }
    }//


    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* To be derived */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * manage the entity
     * 
     * @param body
     */
    public void collectEntity(HttpEntity entity) {
        // to be derived for specific usage
        
    }
    

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Get the result */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public String getBody() {
        return body;
    }
  
    public Object getJson() {
        return body==null ? null : JSONValue.parse(body);
    }
    
    public ByteArrayOutputStream getBaos() {
        return baos;
    }
  
    public List<BEvent> getListEvents() {
        return listEvents;
    }
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Trace */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public String trace() {
        return traceCollect.toString();
    }
}
