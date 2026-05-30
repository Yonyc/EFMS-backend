package yt.wer.efms.dto;

import java.util.List;

public class ShareFilterOptionsDto {

    public static class Option {
        private Long id;
        private String label;

        public Option() {
        }

        public Option(Long id, String label) {
            this.id = id;
            this.label = label;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    private Long shareId;
    private String label;
    private List<Option> periods;
    private List<Option> operationTypes;
    private List<Option> tools;
    private List<Option> products;
    private String filterStartDate;
    private String filterEndDate;

    public Long getShareId() {
        return shareId;
    }

    public void setShareId(Long shareId) {
        this.shareId = shareId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Option> getPeriods() {
        return periods;
    }

    public void setPeriods(List<Option> periods) {
        this.periods = periods;
    }

    public List<Option> getOperationTypes() {
        return operationTypes;
    }

    public void setOperationTypes(List<Option> operationTypes) {
        this.operationTypes = operationTypes;
    }

    public List<Option> getTools() {
        return tools;
    }

    public void setTools(List<Option> tools) {
        this.tools = tools;
    }

    public List<Option> getProducts() {
        return products;
    }

    public void setProducts(List<Option> products) {
        this.products = products;
    }

    public String getFilterStartDate() {
        return filterStartDate;
    }

    public void setFilterStartDate(String filterStartDate) {
        this.filterStartDate = filterStartDate;
    }

    public String getFilterEndDate() {
        return filterEndDate;
    }

    public void setFilterEndDate(String filterEndDate) {
        this.filterEndDate = filterEndDate;
    }
}
