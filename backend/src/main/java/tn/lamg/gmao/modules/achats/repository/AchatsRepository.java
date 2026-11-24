package tn.lamg.gmao.modules.achats.repository;

import org.springframework.stereotype.Repository;
import tn.lamg.gmao.modules.shared.repository.RouteRepository;

@Repository
public class AchatsRepository extends RouteRepository {
    public AchatsRepository() {
        super("suppliers", "inventory/items", "inventory/reservations", "inventory/movements", "purchase-requests", "purchase-orders", "goods-receipts");
    }
}
