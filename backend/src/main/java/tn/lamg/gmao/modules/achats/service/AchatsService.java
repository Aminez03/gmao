package tn.lamg.gmao.modules.achats.service;

import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.achats.model.*;
import tn.lamg.gmao.modules.achats.repository.AchatsRepository;
import tn.lamg.gmao.modules.maintenance.model.WorkOrder;
import tn.lamg.gmao.modules.shared.service.AbstractModuleResourceService;
import tn.lamg.gmao.modules.shared.service.ResourceRules;
import tn.lamg.gmao.security.AccessPolicy;
import tn.lamg.gmao.security.CurrentUser;
import tn.lamg.gmao.service.ResourceService;

@Service
public class AchatsService extends AbstractModuleResourceService implements ResourceRules {

    private final EntityStore store;
    private final AccessPolicy policy;
    private final CurrentUser current;

    public AchatsService(
        AchatsRepository repository,
        ResourceService resources,
        EntityStore store,
        AccessPolicy policy,
        CurrentUser current
    ) {
        super(repository, resources);
        this.store = store;
        this.policy = policy;
        this.current = current;
    }

    @Override
    public boolean supports(BaseEntity entity) {
        return (
            entity instanceof InventoryItem ||
            entity instanceof PurchaseRequest ||
            entity instanceof PurchaseOrder ||
            entity instanceof GoodsReceipt
        );
    }

    @Override
    public void defaults(BaseEntity entity) {
        if (entity instanceof InventoryItem item) {
            item.stock = BigDecimal.ZERO;
            item.reserved = BigDecimal.ZERO;
        }
        if (entity instanceof PurchaseRequest request) {
            request.status = "BROUILLON";
            request.number = store.nextNumber("DA", "purchase_request_display_number_seq");
        }
        if (entity instanceof PurchaseOrder order) order.status = "BROUILLON";
        if (entity instanceof GoodsReceipt receipt) {
            receipt.status = "QUARANTAINE";
            receipt.accepted = BigDecimal.ZERO;
            receipt.rejected = BigDecimal.ZERO;
        }
    }

    @Override
    public void editable(BaseEntity entity) {
        if (entity instanceof PurchaseRequest request) ApiException.require(
            ("BROUILLON".equals(request.status) && request.createdBy.equals(current.get().id)) ||
                (policy.supervisor() && "BROUILLON".equals(request.status)),
            "IMMUTABLE_REQUEST",
            "Seul un brouillon peut être modifié."
        );
        if (entity instanceof PurchaseOrder order) ApiException.require(
            "BROUILLON".equals(order.status),
            "IMMUTABLE_ORDER",
            "Commande déjà envoyée."
        );
        if (entity instanceof GoodsReceipt) throw new ApiException(
            422,
            "QUALITY_COMMAND_REQUIRED",
            "Utilisez le contrôle qualité, sans modifier la réception."
        );
    }

    @Override
    public void validate(BaseEntity entity) {
        if (entity instanceof InventoryItem item) ApiException.require(
            item.minimum.signum() >= 0 && item.unitCost.signum() >= 0,
            "STOCK_VALUES",
            "Seuil et coût doivent être positifs ou nuls."
        );
        if (entity instanceof PurchaseRequest request) {
            ResourceService.positive(request.quantity);
            if (request.workOrderId != null) policy.read(
                store.find(WorkOrder.class, request.workOrderId)
            );
        }
        if (entity instanceof PurchaseOrder order) {
            ResourceService.positive(order.quantity);
            ApiException.require(order.unitPrice.signum() >= 0, "PRICE", "Prix négatif interdit.");
            var request = store.lock(PurchaseRequest.class, order.requestId);
            ApiException.require(
                Set.of("TRANSMISE_ADMIN", "COMMANDEE").contains(request.status),
                "PURCHASE_NOT_APPROVED",
                "La demande doit être approuvée et transmise."
            );
            var total = store
                .all(PurchaseOrder.class)
                .stream()
                .filter(
                    other ->
                        other.requestId.equals(order.requestId) &&
                        !other.id.equals(order.id) &&
                        !"ANNULEE".equals(other.status)
                )
                .map(other -> other.quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            ApiException.require(
                total.add(order.quantity).compareTo(request.quantity) <= 0,
                "OVER_ORDER",
                "Quantité supérieure au besoin."
            );
        }
        if (entity instanceof GoodsReceipt receipt) {
            ResourceService.positive(receipt.quantity);
            var order = store.lock(PurchaseOrder.class, receipt.orderId);
            ApiException.require(
                Set.of("COMMANDEE", "PARTIELLEMENT_RECUE").contains(order.status),
                "ORDER_NOT_PLACED",
                "Commande non active."
            );
            var total = store
                .all(GoodsReceipt.class)
                .stream()
                .filter(other -> other.orderId.equals(receipt.orderId))
                .map(other -> other.quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            ApiException.require(
                total.add(receipt.quantity).compareTo(order.quantity) <= 0,
                "OVER_RECEIPT",
                "Quantité supérieure à la commande."
            );
            order.status =
                total.add(receipt.quantity).compareTo(order.quantity) == 0
                    ? "RECUE"
                    : "PARTIELLEMENT_RECUE";
        }
    }
}
