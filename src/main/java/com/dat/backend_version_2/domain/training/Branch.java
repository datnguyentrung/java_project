package com.dat.backend_version_2.domain.training;

import com.dat.backend_version_2.enums.training.ClassSession.Weekday;
import com.dat.backend_version_2.util.StringListConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Branch", schema = "training")
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idBranch; // className in ClassSession
    private String title;
    private String address;
    private boolean isActive = true;

    @ElementCollection(targetClass = Weekday.class)
    @CollectionTable(
            name = "branch_weekdays",
            joinColumns = @JoinColumn(name = "id_branch"),
            schema = "association"
    )
    @Column(name = "weekday")
    @Enumerated(EnumType.STRING)
    private List<Weekday> weekdays;

    private String avatar;
    private Boolean isNew;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Student> students = new ArrayList<>();

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ClassSession> classSessions = new ArrayList<>();
}
