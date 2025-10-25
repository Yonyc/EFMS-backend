package yt.wer.efms.model;

import java.io.Serializable;
import jakarta.persistence.Embeddable;

@Embeddable
public class FarmUserId implements Serializable {
    private Long farmId;
    private Long userId;

    public FarmUserId() {}

    public FarmUserId(Long farmId, Long userId) {
        this.farmId = farmId;
        this.userId = userId;
    }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FarmUserId that = (FarmUserId) o;
        return java.util.Objects.equals(farmId, that.farmId) && java.util.Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(farmId, userId);
    }
}
