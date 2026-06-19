package com.oddsradar.scheduler;

import com.oddsradar.service.OddsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OddsScheduler {

    private final OddsService oddsService;

    @Scheduled(fixedRate = 60_000)
    @CacheEvict(value = "best-odds", allEntries = true)
    public void refreshOdds() {
        log.info("[OddsScheduler] Atualizando odds...");
        oddsService.getBestOdds("soccer_fifa_world_cup");
        log.info("[OddsScheduler] Odds atualizadas.");
    }
}
