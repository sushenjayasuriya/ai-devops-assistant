package ai.devops.modules.integration.prometheus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrometheusTargetsResponse {

    private String status;
    private TargetData data;

    public PrometheusTargetsResponse() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public TargetData getData() {
        return data;
    }

    public void setData(TargetData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TargetData {
        private List<PrometheusTarget> activeTargets;
        private List<PrometheusTarget> droppedTargets;

        public TargetData() {}

        public List<PrometheusTarget> getActiveTargets() {
            return activeTargets;
        }

        public void setActiveTargets(List<PrometheusTarget> activeTargets) {
            this.activeTargets = activeTargets;
        }

        public List<PrometheusTarget> getDroppedTargets() {
            return droppedTargets;
        }

        public void setDroppedTargets(List<PrometheusTarget> droppedTargets) {
            this.droppedTargets = droppedTargets;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrometheusTarget {
        private Map<String, String> labels;
        private String scrapePool;
        private String scrapeUrl;
        private String health; // "up", "down", "unknown"
        private String lastError;
        private String lastScrape;
        private Double lastScrapeDuration;

        public PrometheusTarget() {}

        public Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }

        public String getScrapePool() {
            return scrapePool;
        }

        public void setScrapePool(String scrapePool) {
            this.scrapePool = scrapePool;
        }

        public String getScrapeUrl() {
            return scrapeUrl;
        }

        public void setScrapeUrl(String scrapeUrl) {
            this.scrapeUrl = scrapeUrl;
        }

        public String getHealth() {
            return health;
        }

        public void setHealth(String health) {
            this.health = health;
        }

        public String getLastError() {
            return lastError;
        }

        public void setLastError(String lastError) {
            this.lastError = lastError;
        }

        public String getLastScrape() {
            return lastScrape;
        }

        public void setLastScrape(String lastScrape) {
            this.lastScrape = lastScrape;
        }

        public Double getLastScrapeDuration() {
            return lastScrapeDuration;
        }

        public void setLastScrapeDuration(Double lastScrapeDuration) {
            this.lastScrapeDuration = lastScrapeDuration;
        }
    }
}
