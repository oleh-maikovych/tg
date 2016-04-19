package ua.com.fielden.platform.sample.domain;

import ua.com.fielden.platform.entity.AbstractFunctionalEntityWithCentreContext;
import ua.com.fielden.platform.entity.annotation.CompanionObject;
import ua.com.fielden.platform.entity.annotation.IsProperty;
import ua.com.fielden.platform.entity.annotation.KeyTitle;
import ua.com.fielden.platform.entity.annotation.KeyType;
import ua.com.fielden.platform.entity.annotation.MapTo;
import ua.com.fielden.platform.entity.annotation.Observable;
import ua.com.fielden.platform.entity.annotation.Title;

/** 
 * An experimental action for exporting data to excel.
 * 
 * @author TG Team
 *
 */
@KeyType(String.class)
@KeyTitle(value = "Key", desc = "Some key description")
@CompanionObject(IExportAction.class)
public class ExportAction extends AbstractFunctionalEntityWithCentreContext<String> {
    private static final long serialVersionUID = 1L;

    @IsProperty
    @MapTo
    @Title(value = "MIME", desc = "File MIME Type")
    private String mime; // application/pdf, application/vnd.ms-excel, text/plain, text/html

    @IsProperty
    @MapTo
    @Title(value = "File Name", desc = "The name of file for the data to be saved into")
    private String fileName;

    @IsProperty
    @Title(value = "Data", desc = "Raw binary data that needs to be persisted.")
    private byte[] data;

    @IsProperty
    @MapTo
    @Title(value = "First # to export", desc = "The number of first matching instances to export.")
    private Integer count;

    @Observable
    public ExportAction setFileName(final String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getFileName() {
        return fileName;
    }
    
    @Observable
    public ExportAction setMime(final String mime) {
        this.mime = mime;
        return this;
    }

    public String getMime() {
        return mime;
    }

    @Observable
    public ExportAction setCount(final Integer count) {
        this.count = count;
        return this;
    }

    public Integer getCount() {
        return count;
    }

    @Observable
    public ExportAction setData(final byte[] data) {
        this.data = data;
        return this;
    }

    public byte[] getData() {
        return data;
    }

}