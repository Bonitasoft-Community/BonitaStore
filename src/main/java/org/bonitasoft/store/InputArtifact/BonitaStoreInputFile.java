package org.bonitasoft.store.InputArtifact;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;

public class BonitaStoreInputFile extends BonitaStoreInput {

    File file;

    public BonitaStoreInputFile(File file) {
        this.file = file;
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public String getContentText() throws Exception {
        BufferedReader br = null;
        FileReader fr = null;
        StringBuffer content = new StringBuffer();
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                content.append(sCurrentLine + "\n");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();
            } catch (Exception e) {
            }
        }
        return content.toString();

    }

    @Override
    public InputStream getContentInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    public Object getSignature() {
        return file;
    }

}
