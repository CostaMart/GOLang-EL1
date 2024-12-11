package util;

import java.util.List;

public class ModelRecord {

    List<String> models;
    String representativeStruct;



    public ModelRecord(List<String> models, String representativeStruct) {
        this.models = models;
        this.representativeStruct = representativeStruct;
    }


    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public String getRepresentativeStruct() {
        return representativeStruct;
    }

    public void setRepresentativeStruct(String representativeStruct) {
        this.representativeStruct = representativeStruct;
    }
}
