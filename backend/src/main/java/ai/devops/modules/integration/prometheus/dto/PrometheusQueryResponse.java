package ai.devops.modules.integration.prometheus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrometheusQueryResponse {

    private String status; // "success" or "error"
    private String errorType;
    private String error;
    private PrometheusData data;

    public PrometheusQueryResponse() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public PrometheusData getData() {
        return data;
    }

    public void setData(PrometheusData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrometheusData {
        private String resultType; // "vector", "matrix", "scalar", "string"
        private List<PrometheusResult> result;

        public PrometheusData() {}

        public String getResultType() {
            return resultType;
        }

        public void setResultType(String resultType) {
            this.resultType = resultType;
        }

        public List<PrometheusResult> getResult() {
            return result;
        }

        public void setResult(List<PrometheusResult> result) {
            this.result = result;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrometheusResult {
        private Map<String, String> metric;
        private List<Object> value; // [epochTimestamp, "valueString"] for instant query
        private List<List<Object>> values; // [[epochTimestamp, "valueString"], ...] for range query

        public PrometheusResult() {}

        public Map<String, String> getMetric() {
            return metric;
        }

        public void setMetric(Map<String, String> metric) {
            this.metric = metric;
        }

        public List<Object> getValue() {
            return value;
        }

        public void setValue(List<Object> value) {
            this.value = value;
        }

        public List<List<Object>> getValues() {
            return values;
        }

        public void setValues(List<List<Object>> values) {
            this.values = values;
        }
    }
}
