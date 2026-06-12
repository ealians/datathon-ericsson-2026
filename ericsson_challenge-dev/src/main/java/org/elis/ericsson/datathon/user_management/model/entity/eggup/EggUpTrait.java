package org.elis.ericsson.datathon.user_management.model.entity.eggup;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elis.ericsson.datathon.user_management.model.modelbase.DateAudit;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "eggup_trait")
public class EggUpTrait extends DateAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trait_id")
    private String traitId;

    @Column(name = "trait_name")
    private String traitName;

    @Column(name = "score")
    private Float score;

    @Column(name = "macro_name")
    private String macroName;

    @Column(name = "macro_score")
    private Float macroScore;

    @Column(name = "macro_weight")
    private Float macroWeight;

    @Column(name = "count")
    private Integer count;

    @ManyToOne()
    @JoinColumn(name = "score_id")
    private EggUpScore eggUpScore;

}
