package com.sap.cloud.lm.sl.cf.persistence.security;

import java.io.File;

public interface VirusScanner {

    public void scanFile(File file) throws VirusScannerException;

}
