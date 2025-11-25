package com.dat.backend_version_2.domain.tournament;

import com.dat.backend_version_2.enums.tournament.AgeDivision;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "AgeGroup", schema = "tournament")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AgeGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAgeGroup;

    private String ageGroupName;
    private Boolean isActive = true;

    private Integer startAge;
    private Integer endAge;
}
