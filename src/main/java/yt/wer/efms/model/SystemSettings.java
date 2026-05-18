package yt.wer.efms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "system_settings")
public class SystemSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_verification_required", nullable = false)
    private boolean userVerificationRequired = false;

    @Column(name = "farm_approval_required", nullable = false)
    private boolean farmApprovalRequired = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public boolean isUserVerificationRequired() { return userVerificationRequired; }
    public void setUserVerificationRequired(boolean userVerificationRequired) { this.userVerificationRequired = userVerificationRequired; }

    public boolean isFarmApprovalRequired() { return farmApprovalRequired; }
    public void setFarmApprovalRequired(boolean farmApprovalRequired) { this.farmApprovalRequired = farmApprovalRequired; }
}
