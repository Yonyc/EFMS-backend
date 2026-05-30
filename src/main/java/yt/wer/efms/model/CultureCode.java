package yt.wer.efms.model;

import jakarta.persistence.*;

/**
 * Official PAC/SIGEC crop classification code.
 * Global reference data not farm-scoped.
 */
@Entity
@Table(name = "culture_codes")
public class CultureCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Official PAC/SIGEC code, e.g. "100". Unique. */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** French label, e.g. "Blé tendre". */
    @Column(name = "label", nullable = false)
    private String label;

    /** Dutch label. */
    @Column(name = "label_nl")
    private String labelNl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getLabelNl() { return labelNl; }
    public void setLabelNl(String labelNl) { this.labelNl = labelNl; }
}
