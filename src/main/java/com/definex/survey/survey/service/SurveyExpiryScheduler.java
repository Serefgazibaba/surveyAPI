package com.definex.survey.survey.service;

import com.definex.survey.survey.repository.SurveyRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bitiş tarihi geçmiş yayındaki anketleri {@code EXPIRED} yapar (dakikada bir).
 * Katılımcı erişiminde durum yerine {@code endsAt} kontrol edileceği için bu gecikme erişimi açık bırakmaz.
 */
@Component
public class SurveyExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SurveyExpiryScheduler.class);

    private final SurveyRepository surveyRepository;
    private final Clock clock;

    public SurveyExpiryScheduler(SurveyRepository surveyRepository, Clock clock) {
        this.surveyRepository = surveyRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.survey.expiry-check-interval:60s}")
    @Transactional
    public void expirePublishedSurveys() {
        int expired = surveyRepository.expirePublished(clock.instant());
        if (expired > 0) {
            log.info("{} anketin süresi doldu (EXPIRED)", expired);
        }
    }
}
