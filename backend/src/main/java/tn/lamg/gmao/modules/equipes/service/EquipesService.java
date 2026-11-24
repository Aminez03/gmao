package tn.lamg.gmao.modules.equipes.service;

import org.springframework.stereotype.Service;
import tn.lamg.gmao.modules.equipes.repository.EquipesRepository;
import tn.lamg.gmao.modules.shared.service.AbstractModuleResourceService;
import tn.lamg.gmao.service.ResourceService;

@Service
public class EquipesService extends AbstractModuleResourceService {
    public EquipesService(EquipesRepository repository, ResourceService resources) {
        super(repository, resources);
    }
}
