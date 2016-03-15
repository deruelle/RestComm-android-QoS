package com.cortxt.app.MMC.Sampling.Transit;

import java.io.Serializable;
import java.util.ArrayList;

public class TransitInfoWrapper implements Serializable {

    private static final long serialVersionUID = 1L;
    private ArrayList<TransitInfo> itemDetails;

    public TransitInfoWrapper(ArrayList<TransitInfo> items) {
        this.itemDetails = items;
    }

    public ArrayList<TransitInfo> getItemDetails() {
        return itemDetails;
    }
}