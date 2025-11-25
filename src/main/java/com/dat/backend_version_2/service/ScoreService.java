package com.dat.backend_version_2.service;

import com.dat.backend_version_2.dto.ScoreDTO;
import com.dat.backend_version_2.dto.attendance.AttendanceDTO;
import com.dat.backend_version_2.dto.attendance.StudentAttendanceDTO;
import com.dat.backend_version_2.enums.attendance.AttendanceStatus;
import com.dat.backend_version_2.enums.attendance.EvaluationStatus;
import com.dat.backend_version_2.service.attendance.StudentAttendanceService;
import com.dat.backend_version_2.util.ScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreService {
    private final StudentAttendanceService studentAttendanceService;

    public ScoreDTO.SummaryScore getSummaryByQuarter(int year, int quarter, String idAccount) {
        // 1. Gọi phương thức để lấy List<StudentAttendanceDTO.StudentAttendanceDetail>
        List<StudentAttendanceDTO.StudentAttendanceDetail> detailList = studentAttendanceService
                .getAttendancesByQuarter(idAccount, year, quarter);

        // 2. Sử dụng stream để ánh xạ (map) từng StudentAttendanceDetail
        //    sang kiểu cha là AttendanceDTO.AttendanceInfo
        List<AttendanceDTO.AttendanceInfo> attendanceInfos = detailList.stream()
                // Ép kiểu (cast) từng phần tử sang kiểu cha
                .map(detail -> (AttendanceDTO.AttendanceInfo) detail)
                .toList();

        ScoreDTO.AchievementScore achievementScore = new ScoreDTO.AchievementScore();
        ScoreDTO.ContributionScore contributionScore = new ScoreDTO.ContributionScore();

        ScoreDTO.BonusScore bonusScore = getBonusScore(achievementScore, contributionScore);

        ScoreDTO.SummaryScore summaryScore = getAssessmentScore(attendanceInfos);
        summaryScore.setBonusScore(bonusScore);
        summaryScore.setScore(ScoreCalculator.calculateSummaryScore(
                summaryScore.getConductScore(),
                summaryScore.getAwarenessScore(),
                summaryScore.getBonusScore()
        ));
        return summaryScore;
    }

    public ScoreDTO.BonusScore getBonusScore(
            ScoreDTO.AchievementScore achievementScore,
            ScoreDTO.ContributionScore contributionScore){
        if (achievementScore == null || contributionScore == null){
            throw new IllegalArgumentException("AchievementScore and ContributionScore must not be null");
        }
        ScoreDTO.BonusScore bonusScore = new ScoreDTO.BonusScore();
        bonusScore.setAchievementScore(achievementScore);
        bonusScore.setContributionScore(contributionScore);
        bonusScore.setScore(ScoreCalculator.calculateBonusScore(bonusScore));
        return bonusScore;
    }

    public ScoreDTO.SummaryScore getAssessmentScore(List<AttendanceDTO.AttendanceInfo> studentAttendanceList) {
        if (studentAttendanceList == null || studentAttendanceList.isEmpty()) {
            return new ScoreDTO.SummaryScore();
        }

        // Khởi tạo các object con
        ScoreDTO.ConductScore conductScore = new ScoreDTO.ConductScore();
        ScoreDTO.AwarenessScore awarenessScore = new ScoreDTO.AwarenessScore();

        ScoreDTO.BonusScore bonusScore = new ScoreDTO.BonusScore();
        ScoreDTO.AchievementScore achievementScore = new ScoreDTO.AchievementScore();
        ScoreDTO.ContributionScore contributionScore = new ScoreDTO.ContributionScore();

        ScoreDTO.SummaryScore summaryScore = new ScoreDTO.SummaryScore();

        summaryScore.setConductScore(conductScore);
        summaryScore.setAwarenessScore(awarenessScore);
        summaryScore.setBonusScore(bonusScore);

        bonusScore.setAchievementScore(achievementScore);
        bonusScore.setContributionScore(contributionScore);

        for (AttendanceDTO.AttendanceInfo attendanceInfo : studentAttendanceList) {
            AttendanceDTO.AttendanceDetail attendanceDetail = attendanceInfo.getAttendance();
            // Xử lý từng bản ghi điểm danh
            if (attendanceDetail.getAttendanceStatus() == null) continue;
            switch (attendanceDetail.getAttendanceStatus()) {
                case AttendanceStatus.V: // Vắng
                    conductScore.setAbsentSession(conductScore.getAbsentSession() + 1);
                    break;
                case AttendanceStatus.M: // Muộn
                    conductScore.setLate(conductScore.getLate() + 1);
                    conductScore.setTrainingSession(conductScore.getTrainingSession() + 1);
                    break;
                case AttendanceStatus.P: // Phép
                    conductScore.setExcusedAbsence(conductScore.getExcusedAbsence() + 1);
                    break;
                case AttendanceStatus.B: // Tập bù
                    conductScore.setCompensatorySession(conductScore.getCompensatorySession() + 1);
                    conductScore.setTrainingSession(conductScore.getTrainingSession() + 1);
                    break;
                case AttendanceStatus.X: // Rèn luyện
                    conductScore.setTrainingSession(conductScore.getTrainingSession() + 1);
                    break;
                default:
                    // Xử lý các trạng thái khác nếu cần
                    break;
            }

            AttendanceDTO.EvaluationDetail evaluationDetail = attendanceInfo.getEvaluation();
            if (evaluationDetail.getEvaluationStatus() == null) continue;
            switch (evaluationDetail.getEvaluationStatus()) {
                case EvaluationStatus.T -> awarenessScore.setHighScore(awarenessScore.getHighScore() + 1);
                case EvaluationStatus.TB -> awarenessScore.setMediumScore(awarenessScore.getMediumScore() + 1);
                case EvaluationStatus.Y -> awarenessScore.setLowScore(awarenessScore.getLowScore() + 1);
            }
        }

        conductScore.setScore(ScoreCalculator.calculateConduct(conductScore));
        awarenessScore.setScore(ScoreCalculator.calculateAwareness(awarenessScore));
        return summaryScore;
    }
}
