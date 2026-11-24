package tn.lamg.gmao.modules.pilotage.service;

import org.springframework.stereotype.Service;
import tn.lamg.gmao.modules.pilotage.repository.PilotageRepository;
import tn.lamg.gmao.modules.shared.service.AbstractModuleResourceService;
import tn.lamg.gmao.service.ResourceService;

@Service
public class PilotageService extends AbstractModuleResourceService {

    public PilotageService(PilotageRepository repository, ResourceService resources) {
        super(repository, resources);
    }
}
