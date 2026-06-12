package org.elis.ericsson.datathon.user_management.model.entity.eggup;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.RandomStringUtils;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.modelbase.DateAudit;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "eggup_user")
public class EggUpInfo extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eggup_user_guid")
    private Long eggUpUserGuid;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "assessment_url")
    private String assessmentUrl;

    @Column(name = "authentication_token")
    private String authenticationToken;

    @OneToOne(optional = true)
    @JoinColumn(name = "score_id", nullable = true)
    private EggUpScore eggUpScore;

    @OneToOne(optional = false)
    @JoinColumn(name = "eni_user_id", nullable = false)
    private UserProfile creationUser;

    public EggUpInfo(UserProfile user) {
        this.creationUser = user;
        this.username = String.format("user_%s", user.getId());
        this.password = RandomStringUtils.random(15, true, true);
    }

}
