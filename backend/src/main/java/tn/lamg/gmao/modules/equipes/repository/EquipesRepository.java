package tn.lamg.gmao.modules.equipes.repository;

import org.springframework.stereotype.Repository;
import tn.lamg.gmao.modules.shared.repository.RouteRepository;

@Repository
public class EquipesRepository extends RouteRepository {
    public EquipesRepository() {
        super("teams", "skills", "user-skills", "team-members", "shifts", "shift-assignments", "absences", "handovers");
    }
}
