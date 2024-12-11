package util;

public class DatasetRecord {
    private String[] header;
    private String[] headerTypes;


    public DatasetRecord(String[] header, String[] headerTypes) {
        this.header = header;
        this.headerTypes = headerTypes;
    }

    public String[] getHeader() {
        return header;
    }

    public String[] getHeaderTypes() {
        return headerTypes;
    }

    public void setHeaderTypes(String[] headerTypes) {
        this.headerTypes = headerTypes;
    }
}
