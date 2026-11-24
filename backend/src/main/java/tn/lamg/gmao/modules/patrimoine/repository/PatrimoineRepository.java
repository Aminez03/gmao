package tn.lamg.gmao.modules.patrimoine.repository;

import org.springframework.stereotype.Repository;
import tn.lamg.gmao.modules.shared.repository.RouteRepository;

@Repository
public class PatrimoineRepository extends RouteRepository {

    public PatrimoineRepository() {
        super("locations", "assets", "attachments");
    }
}
