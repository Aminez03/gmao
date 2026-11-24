package tn.lamg.gmao.modules.maintenance.service;

import org.springframework.stereotype.Service;
import tn.lamg.gmao.modules.maintenance.repository.MaintenanceRepository;
import tn.lamg.gmao.modules.shared.service.AbstractModuleResourceService;
import tn.lamg.gmao.service.ResourceService;

@Service
public class MaintenanceService extends AbstractModuleResourceService {

    public MaintenanceService(MaintenanceRepository repository, ResourceService resources) {
        super(repository, resources);
    }
}
