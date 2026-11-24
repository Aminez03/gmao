package tn.lamg.gmao.modules.pilotage.repository;

import org.springframework.stereotype.Repository;
import tn.lamg.gmao.modules.shared.repository.RouteRepository;

@Repository
public class PilotageRepository extends RouteRepository {
    public PilotageRepository() {
        super("notifications", "audit-events");
    }
}
