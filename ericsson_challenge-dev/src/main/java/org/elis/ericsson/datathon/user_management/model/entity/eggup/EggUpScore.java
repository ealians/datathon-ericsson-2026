package org.elis.ericsson.datathon.user_management.model.entity.eggup;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elis.ericsson.datathon.user_management.model.modelbase.DateAudit;

import java.time.LocalDateTime;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "eggup_score")
public class EggUpScore extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_name")
    private String testName;

    @Column(name = "coverage_index")
    private Float coverageIndex;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "date")
    private LocalDateTime date;

    @Nullable
    @OneToOne(mappedBy = "eggUpScore", optional = true)
    private EggUpInfo eggUpInfo;

    @OneToMany(mappedBy = "eggUpScore")
    private Set<EggUpTrait> eggUpTrait;


}
