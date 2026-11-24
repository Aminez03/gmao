package tn.lamg.gmao.modules.prestataires.service;

import org.springframework.stereotype.Service;
import tn.lamg.gmao.modules.prestataires.repository.PrestatairesRepository;
import tn.lamg.gmao.modules.shared.service.AbstractModuleResourceService;
import tn.lamg.gmao.service.ResourceService;

@Service
public class PrestatairesService extends AbstractModuleResourceService {
    public PrestatairesService(PrestatairesRepository repository, ResourceService resources) {
        super(repository, resources);
    }
}
