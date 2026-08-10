package org.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "app_config", schema = "application")
public class AppConfig {

    @Id
    private Short id;

    @Column(name = "report_threshold", nullable = false)
    private int reportThreshold;

    @Column(name = "daily_report_limit", nullable = false)
    private int dailyReportLimit;
}
