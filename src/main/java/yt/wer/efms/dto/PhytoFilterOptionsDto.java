package yt.wer.efms.dto;

import java.util.List;

public class PhytoFilterOptionsDto {
    private List<LabeledValue> decisionCodes;
    private List<LabeledValue> userGroups;
    private List<LabeledValue> productTypes;

    public PhytoFilterOptionsDto(List<LabeledValue> decisionCodes, List<LabeledValue> userGroups, List<LabeledValue> productTypes) {
        this.decisionCodes = decisionCodes;
        this.userGroups = userGroups;
        this.productTypes = productTypes;
    }

    public List<LabeledValue> getDecisionCodes() { return decisionCodes; }
    public List<LabeledValue> getUserGroups() { return userGroups; }
    public List<LabeledValue> getProductTypes() { return productTypes; }

    public record LabeledValue(String value, String label) {}
}
