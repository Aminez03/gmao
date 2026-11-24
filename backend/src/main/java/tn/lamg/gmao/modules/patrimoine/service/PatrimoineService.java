package tn.lamg.gmao.modules.patrimoine.service;

import org.springframework.stereotype.Service;
import tn.lamg.gmao.modules.patrimoine.repository.PatrimoineRepository;
import tn.lamg.gmao.modules.shared.service.AbstractModuleResourceService;
import tn.lamg.gmao.service.ResourceService;

@Service
public class PatrimoineService extends AbstractModuleResourceService {

    public PatrimoineService(PatrimoineRepository repository, ResourceService resources) {
        super(repository, resources);
    }
}
