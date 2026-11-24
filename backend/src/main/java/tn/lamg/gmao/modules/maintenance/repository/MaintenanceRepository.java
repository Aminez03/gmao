package tn.lamg.gmao.modules.maintenance.repository;

import org.springframework.stereotype.Repository;
import tn.lamg.gmao.modules.shared.repository.RouteRepository;

@Repository
public class MaintenanceRepository extends RouteRepository {
    public MaintenanceRepository() {
        super("checklists", "preventive-plans", "preventive-occurrences", "work-orders", "failure-reports", "time-entries", "anomalies", "downtimes");
    }
}
