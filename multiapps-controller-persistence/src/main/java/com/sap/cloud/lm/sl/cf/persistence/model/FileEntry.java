package com.sap.cloud.lm.sl.cf.persistence.model;

import java.math.BigInteger;
import java.util.Date;

public class FileEntry {

    private String id;

    private String name;

    private String namespace;

    private String space;

    private BigInteger size;

    private String digest;

    private String digestAlgorithm;

    private Date modified;

    public FileEntry() {
        super();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public BigInteger getSize() {
        return size;
    }

    public void setSize(BigInteger size) {
        this.size = size;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    @Override
    public String toString() {
        return "FileEntry [id=" + id + ", name=" + name + ", namespace=" + namespace + ", space=" + space + ", digest=" + digest
            + ", modified=" + modified + "]";
    }

}
