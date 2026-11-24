package tn.lamg.gmao.modules.prestataires.repository;

import org.springframework.stereotype.Repository;
import tn.lamg.gmao.modules.shared.repository.RouteRepository;

@Repository
public class PrestatairesRepository extends RouteRepository {
    public PrestatairesRepository() {
        super("subcontractors", "contracts", "external-interventions", "material-requests");
    }
}
