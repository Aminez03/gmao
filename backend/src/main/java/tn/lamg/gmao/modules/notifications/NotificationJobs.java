package tn.lamg.gmao.modules.notifications;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationJobs {

    final JdbcTemplate jdbc;

    public NotificationJobs(JdbcTemplate j) {
        jdbc = j;
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    @Transactional
    public void reminders() {
        alert(
            "Intervention en retard",
            "work-orders",
            "work_order",
            "status not in ('VALIDEE','ANNULEE') and due_at<now()"
        );
        alert(
            "Panne critique active",
            "work-orders",
            "work_order",
            "status not in ('VALIDEE','ANNULEE') and priority='P1'"
        );
        alert(
            "Consigne sans accusÃ© de rÃ©ception",
            "handovers",
            "handover",
            "acknowledged_at is null and r.created_at<now()-interval '15 minutes'"
        );
        alert(
            "Stock sous le seuil minimum",
            "inventory/items",
            "inventory_item",
            "stock-reserved<minimum"
        );
    }

    private void alert(String title, String route, String table, String predicate) {
        // Table/predicate values are private constants, never caller-provided SQL.
        jdbc.update(
            "insert into notification(id,version,created_at,updated_at,archived,recipient_id,title,resource_type,resource_id) select gen_random_uuid(),0,now(),now(),false,u.id,?,?,r.id from " +
                table +
                " r cross join app_user u where r.archived=false and u.active=true and u.role in ('ADMIN','RESPONSABLE_TECHNIQUE') and " +
                predicate +
                " on conflict do nothing",
            title,
            route
        );
    }
}
