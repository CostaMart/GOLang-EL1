package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class FunctionRecord {



    List<String> params = new ArrayList<>();
    List<String> returnVals = new ArrayList<>();

    public FunctionRecord(String params) {
        String[] s = params.split("-");
        params = s[0].replace("(", "");
        params = params.replace(")", "");
        this.params = Arrays.stream(params.split(",")).toList();

        String[] toReturn = {};

        if (s.length > 1) {
            toReturn = s[1].split(",");
        }
        this.returnVals = Arrays.stream(toReturn).toList();
    }


    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public List<String> getReturnVals() {
        return returnVals;
    }

    public void setReturnVals(List<String> returnVals) {
        this.returnVals = returnVals;
    }




}
