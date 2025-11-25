package com.dat.backend_version_2.service.tournament;

import com.dat.backend_version_2.domain.tournament.AgeGroup;
import com.dat.backend_version_2.domain.tournament.BeltGroup;
import com.dat.backend_version_2.domain.tournament.Poomsae.PoomsaeCombination;
import com.dat.backend_version_2.domain.tournament.Poomsae.PoomsaeContent;
import com.dat.backend_version_2.dto.tournament.AgeGroupDTO;
import com.dat.backend_version_2.repository.tournament.AgeGroupRepository;
import com.dat.backend_version_2.repository.tournament.BeltGroupRepository;
import com.dat.backend_version_2.repository.tournament.Poomsae.PoomsaeCombinationRepository;
import com.dat.backend_version_2.service.tournament.Poomsae.PoomsaeContentService;
import com.dat.backend_version_2.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgeGroupService {
    private final AgeGroupRepository ageGroupRepository;
    private final BeltGroupRepository beltGroupRepository;
    private final PoomsaeContentService poomsaeContentService;
    private final PoomsaeCombinationRepository poomsaeCombinationRepository;

    public List<AgeGroup> getAllAgeGroups() {
        return ageGroupRepository.findAll();
    }

    public AgeGroup getAgeGroupById(int id) throws IdInvalidException {
        return ageGroupRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Age group not found with id: " + id));
    }

    public AgeGroup update(AgeGroup ageGroup) {
        return ageGroupRepository.save(ageGroup);
    }

    public void deleteAgeGroup(int id) {
        ageGroupRepository.deleteById(id);
    }

    public AgeGroup createAgeGroup(AgeGroupDTO ageGroupDTO) {
        // 1️⃣ Tạo mới AgeGroup
        AgeGroup ageGroup = new AgeGroup();
        ageGroup.setAgeGroupName(ageGroupDTO.getAgeGroupName());
        ageGroup.setStartAge(ageGroupDTO.getStartAge());
        ageGroup.setEndAge(ageGroupDTO.getEndAge());
        ageGroup.setIsActive(true);
        ageGroupRepository.save(ageGroup);

        // 2️⃣ Lấy toàn bộ BeltGroup và PoomsaeContent hiện có
        List<BeltGroup> beltGroups = beltGroupRepository.findAll();
        List<PoomsaeContent> poomsaeContents = poomsaeContentService.getAllPoomsaeContent();

        List<PoomsaeCombination> poomsaeCombinations = new ArrayList<>();

        // 3️⃣ Tạo tổ hợp cho từng nội dung thi đấu và nhóm đai
        for (PoomsaeContent poomsaeContent : poomsaeContents) {
            for (BeltGroup beltGroup : beltGroups) {
                PoomsaeCombination combination = new PoomsaeCombination();
                combination.setPoomsaeContent(poomsaeContent);
                combination.setAgeGroup(ageGroup);
                combination.setBeltGroup(beltGroup);
                poomsaeCombinations.add(combination);
            }
        }

        // 4️⃣ Lưu toàn bộ combination vào DB
        poomsaeCombinationRepository.saveAll(poomsaeCombinations);

        return ageGroup;
    }

}
