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

    // Atualiza a cada 10 minutos — preserva cota da The Odds API
   @Scheduled(fixedRate = 1_800_000) // 30 minutos
    @CacheEvict(value = "best-odds", allEntries = true)
    public void refreshOdds() {
        log.info("[OddsScheduler] Atualizando odds...");
        try {
            oddsService.getBestOdds("soccer_fifa_world_cup");
            log.info("[OddsScheduler] Odds atualizadas com sucesso.");
        } catch (Exception e) {
            log.error("[OddsScheduler] Erro ao atualizar odds: {}", e.getMessage());
        }
    }
}
